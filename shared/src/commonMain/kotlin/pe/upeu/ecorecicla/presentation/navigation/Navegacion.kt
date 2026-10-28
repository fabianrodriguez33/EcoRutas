package pe.upeu.ecorecicla.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.usecase.ObservarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.RestaurarSesionUseCase

sealed interface Destino {
    data object Login : Destino
    data object Ruta : Destino
    data class Detalle(val puntoId: Long) : Destino
    data class Formulario(val puntoId: Long, val rutaId: Long, val pesajeId: Long? = null) : Destino
    data object Perfil : Destino
}

/** Pila de navegación única de la aplicación; las pantallas piden cambios, no se conocen entre sí. */
class Navegador {
    private val pila = MutableStateFlow<List<Destino>>(listOf(Destino.Login))
    val destino: StateFlow<List<Destino>> = pila.asStateFlow()

    fun ir(destino: Destino) {
        pila.value = pila.value + destino
    }

    /** Retrocede un paso; devuelve false si ya estaba en la raíz (la actividad decide cerrar). */
    fun atras(): Boolean {
        if (pila.value.size <= 1) return false
        pila.value = pila.value.dropLast(1)
        return true
    }

    fun reiniciar(destino: Destino) {
        pila.value = listOf(destino)
    }
}

/** Decide la pantalla raíz según la sesión: entra directo si el token guardado sigue vigente. */
class RaizViewModel(
    private val navegador: Navegador,
    restaurarSesion: RestaurarSesionUseCase,
    observarSesion: ObservarSesionUseCase
) : ViewModel() {

    private val _listo = MutableStateFlow(false)
    val listo: StateFlow<Boolean> = _listo.asStateFlow()

    init {
        val sesion = restaurarSesion()
        navegador.reiniciar(if (sesion != null) Destino.Ruta else Destino.Login)
        _listo.value = true
        viewModelScope.launch {
            // Cualquier cambio posterior (inicio de sesión, cierre o un 401) reubica la pantalla raíz.
            observarSesion().collect { actual ->
                val enLogin = navegador.destino.value.last() == Destino.Login
                when {
                    actual == null && !enLogin -> navegador.reiniciar(Destino.Login)
                    actual != null && enLogin -> navegador.reiniciar(Destino.Ruta)
                }
            }
        }
    }
}
