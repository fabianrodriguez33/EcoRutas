package pe.upeu.ecorecicla.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.Privilegios
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.usecase.AnularPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPesajesUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPuntoUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.comoErrorApi
import pe.upeu.ecorecicla.presentation.mensajeUsuario

data class DetalleUiState(
    val punto: PuntoAcopio? = null,
    val pesajes: List<Pesaje> = emptyList(),
    val cargando: Boolean = true,
    val rutaId: Long = 0,
    val rutaCerrada: Boolean = false,
    val sesion: Sesion? = null,
    val trabajando: Boolean = false,
    val aviso: String? = null
) {
    private fun tiene(privilegio: String) = sesion?.tiene(privilegio) == true

    // Los botones se ocultan por experiencia de uso; el servicio rechaza igualmente lo no permitido.
    val puedeRegistrar: Boolean get() = tiene(Privilegios.PESAJE_CREAR) && !rutaCerrada
    val puedeAnular: Boolean get() = tiene(Privilegios.PESAJE_ANULAR) && !rutaCerrada

    fun puedeCorregir(pesaje: Pesaje): Boolean =
        tiene(Privilegios.PESAJE_EDITAR) && !rutaCerrada &&
            (tiene(Privilegios.PESAJE_EDITAR_AJENOS) || pesaje.operario == sesion?.usuario)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleViewModel(
    private val observarPunto: ObservarPuntoUseCase,
    private val observarPesajes: ObservarPesajesUseCase,
    private val observarRuta: ObservarRutaUseCase,
    private val observarSesion: ObservarSesionUseCase,
    private val anularPesaje: AnularPesajeUseCase,
    private val sincronizar: SincronizarUseCase
) : ViewModel() {

    private val _estado = MutableStateFlow(DetalleUiState())
    val estado: StateFlow<DetalleUiState> = _estado

    private var observando: Job? = null
    private var puntoActual: Long? = null

    fun cargar(puntoId: Long) {
        if (puntoActual == puntoId) return
        puntoActual = puntoId
        observando?.cancel()
        observando = viewModelScope.launch {
            combine(observarPunto(puntoId), observarPesajes(puntoId), observarRuta(), observarSesion()) { punto, pesajes, ruta, sesion ->
                Arreglo(punto, pesajes, ruta?.id ?: 0, ruta?.cerrada == true, sesion)
            }.collect { a ->
                _estado.update {
                    it.copy(
                        punto = a.punto, pesajes = a.pesajes, cargando = false, rutaId = a.rutaId,
                        rutaCerrada = a.rutaCerrada, sesion = a.sesion
                    )
                }
            }
        }
    }

    fun anular(pesajeId: Long, motivo: String) {
        if (_estado.value.trabajando) return
        _estado.update { it.copy(trabajando = true) }
        viewModelScope.launch {
            anularPesaje(pesajeId, motivo).fold(
                onSuccess = { resultado ->
                    val texto = if (resultado.enviado) "Pesaje anulado" else "Anulación guardada; se enviará al sincronizar"
                    _estado.update { it.copy(trabajando = false, aviso = texto) }
                },
                onFailure = { fallo ->
                    val error = fallo.comoErrorApi()
                    _estado.update { it.copy(trabajando = false, aviso = error.mensajeUsuario()) }
                    // 404: el registro ya no existe, se refresca el listado sin ofrecer reintento.
                    if (error is ErrorApi.NoEncontrado) sincronizar()
                }
            )
        }
    }

    fun descartarAviso() = _estado.update { it.copy(aviso = null) }

    private class Arreglo(
        val punto: PuntoAcopio?, val pesajes: List<Pesaje>, val rutaId: Long, val rutaCerrada: Boolean, val sesion: Sesion?
    )
}
