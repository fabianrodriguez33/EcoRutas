package pe.upeu.ecorecicla.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.usecase.CorregirPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPuntoUseCase
import pe.upeu.ecorecicla.domain.usecase.RegistrarPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.comoErrorApi
import pe.upeu.ecorecicla.presentation.mensajeUsuario

data class FormularioUiState(
    val esEdicion: Boolean = false,
    val cargando: Boolean = true,
    val material: String = "",
    val pesoBruto: String = "",
    val tara: String = "",
    val humedad: String = "",
    val observacion: String = "",
    val errores: Map<String, String> = emptyMap(),
    val guardando: Boolean = false,
    val aviso: String? = null,
    val terminado: Boolean = false
)

class FormularioViewModel(
    private val observarPunto: ObservarPuntoUseCase,
    private val observarPesaje: ObservarPesajeUseCase,
    private val registrarPesaje: RegistrarPesajeUseCase,
    private val corregirPesaje: CorregirPesajeUseCase,
    private val sincronizar: SincronizarUseCase
) : ViewModel() {

    private val _estado = MutableStateFlow(FormularioUiState())
    val estado: StateFlow<FormularioUiState> = _estado

    private var puntoId = 0L
    private var rutaId = 0L
    private var pesajeId: Long? = null
    private var iniciado = false

    fun iniciar(puntoId: Long, rutaId: Long, pesajeId: Long?) {
        if (iniciado) return
        iniciado = true
        this.puntoId = puntoId
        this.rutaId = rutaId
        this.pesajeId = pesajeId
        viewModelScope.launch {
            if (pesajeId != null) {
                val pesaje = observarPesaje(pesajeId).first()
                _estado.update {
                    if (pesaje == null) it.copy(cargando = false, esEdicion = true, aviso = "El pesaje ya no existe")
                    else it.copy(
                        cargando = false, esEdicion = true, material = pesaje.material,
                        pesoBruto = pesaje.pesoBruto.aTexto(), tara = pesaje.tara.aTexto(),
                        humedad = pesaje.humedadEstimada?.aTexto().orEmpty(),
                        observacion = pesaje.observacion.orEmpty()
                    )
                }
            } else {
                val punto = observarPunto(puntoId).first()
                _estado.update { it.copy(cargando = false, material = punto?.materialHabitual.orEmpty()) }
            }
        }
    }

    fun cambiarMaterial(v: String) = _estado.update { it.copy(material = v, errores = it.errores - "material") }
    fun cambiarPesoBruto(v: String) = _estado.update { it.copy(pesoBruto = v, errores = it.errores - "pesoBruto") }
    fun cambiarTara(v: String) = _estado.update { it.copy(tara = v, errores = it.errores - "tara" - "pesoBruto") }
    fun cambiarHumedad(v: String) = _estado.update { it.copy(humedad = v, errores = it.errores - "humedadEstimada") }
    fun cambiarObservacion(v: String) = _estado.update { it.copy(observacion = v, errores = it.errores - "observacion") }
    fun descartarAviso() = _estado.update { it.copy(aviso = null) }

    fun guardar() {
        val actual = _estado.value
        if (actual.guardando || actual.cargando) return
        _estado.update { it.copy(guardando = true, errores = emptyMap(), aviso = null) }
        viewModelScope.launch {
            val id = pesajeId
            val resultado = if (id == null) {
                registrarPesaje(puntoId, rutaId, actual.material, actual.pesoBruto, actual.tara, actual.observacion, actual.humedad)
            } else {
                corregirPesaje(id, actual.material, actual.pesoBruto, actual.tara, actual.observacion, actual.humedad)
            }
            resultado.fold(
                onSuccess = { _estado.update { it.copy(guardando = false, terminado = true) } },
                onFailure = { fallo ->
                    val error = fallo.comoErrorApi()
                    _estado.update {
                        it.copy(
                            guardando = false,
                            // El 400 se muestra bajo el campo que lo originó; el resto, como aviso con su motivo.
                            errores = (error as? ErrorApi.Validacion)?.campos.orEmpty(),
                            aviso = if (error is ErrorApi.Validacion) null else error.mensajeUsuario(),
                            terminado = error is ErrorApi.NoEncontrado
                        )
                    }
                    if (error is ErrorApi.NoEncontrado) sincronizar()
                }
            )
        }
    }

    private fun Double.aTexto(): String = if (this == this.toLong().toDouble()) toLong().toString() else toString()
}
