package pe.upeu.ecorecicla.presentation.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.repository.Reloj
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Privilegios
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.ResumenSync
import pe.upeu.ecorecicla.domain.model.RutaInfo
import pe.upeu.ecorecicla.domain.usecase.CerrarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarConexionUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPendientesUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPuntosUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.comoErrorApi
import pe.upeu.ecorecicla.presentation.mensajeUsuario
import pe.upeu.ecorecicla.presentation.permiteReintento

data class RutaUiState(
    val ruta: RutaInfo? = null,
    val puntos: List<PuntoAcopio> = emptyList(),
    val busqueda: String = "",
    val sincronizando: Boolean = false,
    val resumen: ResumenSync? = null,
    val ultimaSincronizacion: String? = null,
    val errorMensaje: String? = null,
    val permiteReintento: Boolean = false,
    val aviso: String? = null,
    val puedeCerrarRuta: Boolean = false,
    val usuario: String = "",
    val pendientes: Int = 0
) {
    /** Carga inicial: se está trayendo la ruta por primera vez y aún no hay nada local que mostrar. */
    val cargando: Boolean get() = sincronizando && ruta == null
    val sinRuta: Boolean get() = !sincronizando && ruta == null && errorMensaje == null
    val errorSinDatos: Boolean get() = !sincronizando && ruta == null && errorMensaje != null
}

private data class Transitorio(
    val sincronizando: Boolean = false,
    val resumen: ResumenSync? = null,
    val error: ErrorApi? = null,
    val aviso: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class RutaViewModel(
    observarRuta: ObservarRutaUseCase,
    observarPuntos: ObservarPuntosUseCase,
    observarSesion: ObservarSesionUseCase,
    observarPendientes: ObservarPendientesUseCase,
    observarConexion: ObservarConexionUseCase,
    private val sincronizar: SincronizarUseCase,
    private val cerrarRuta: CerrarRutaUseCase,
    private val reloj: Reloj
) : ViewModel() {

    private val busqueda = MutableStateFlow("")
    private val transitorio = MutableStateFlow(Transitorio())

    private val puntos = busqueda.flatMapLatest { observarPuntos(it) }

    val estado: StateFlow<RutaUiState> = combine(
        observarRuta(), puntos, busqueda, transitorio, observarSesion()
    ) { ruta, lista, texto, temporal, sesion ->
        RutaUiState(
            ruta = ruta,
            puntos = lista,
            busqueda = texto,
            sincronizando = temporal.sincronizando,
            resumen = temporal.resumen,
            ultimaSincronizacion = ruta?.ultimaSincronizacion?.let { reloj.formatearHora(it) },
            errorMensaje = temporal.error?.mensajeUsuario(),
            permiteReintento = temporal.error?.permiteReintento ?: false,
            aviso = temporal.aviso,
            // El control de seguridad real está en el servicio; aquí solo se evita ofrecer lo no permitido.
            puedeCerrarRuta = sesion?.tiene(Privilegios.RUTA_CERRAR) == true && ruta != null && !ruta.cerrada,
            usuario = sesion?.nombre.orEmpty()
        )
    }.combine(observarPendientes()) { estado, pendientes ->
        // El número sale de una consulta observable de la base local, no de un contador en el ViewModel.
        estado.copy(pendientes = pendientes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RutaUiState())

    init {
        // Sincronización automática: al abrir la pantalla y cada vez que se recupera la conexión.
        viewModelScope.launch {
            observarConexion().distinctUntilChanged().filter { it }.collect { sincronizarAhora() }
        }
    }

    fun buscar(texto: String) {
        busqueda.value = texto
    }

    fun reintentar() = sincronizarAhora()

    fun sincronizarAhora() {
        if (transitorio.value.sincronizando) return
        transitorio.update { it.copy(sincronizando = true, error = null) }
        viewModelScope.launch {
            sincronizar().fold(
                onSuccess = { resumen -> transitorio.update { it.copy(sincronizando = false, resumen = resumen) } },
                onFailure = { fallo ->
                    val error = fallo.comoErrorApi()
                    // Un 401 ya limpió la sesión y la raíz vuelve al inicio de sesión: no se muestra error.
                    transitorio.update {
                        it.copy(sincronizando = false, error = error.takeUnless { e -> e is ErrorApi.NoAutorizado })
                    }
                }
            )
        }
    }

    fun cerrarLaRuta() {
        viewModelScope.launch {
            cerrarRuta().fold(
                onSuccess = { transitorio.update { it.copy(aviso = "La ruta del día quedó cerrada") } },
                onFailure = { fallo -> transitorio.update { it.copy(aviso = fallo.comoErrorApi().mensajeUsuario()) } }
            )
        }
    }

    fun descartarAviso() = transitorio.update { it.copy(aviso = null) }
}
