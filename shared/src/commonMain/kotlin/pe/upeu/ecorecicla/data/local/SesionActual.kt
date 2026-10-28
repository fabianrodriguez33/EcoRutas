package pe.upeu.ecorecicla.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pe.upeu.ecorecicla.domain.model.Sesion

/** Estado único de la sesión: lo lee el cliente HTTP para la cabecera y lo observa la interfaz. */
class SesionActual(private val almacen: AlmacenSeguro) {
    private val estado = MutableStateFlow<Sesion?>(null)
    val sesion: StateFlow<Sesion?> = estado

    fun token(): String? = estado.value?.token

    fun establecer(sesion: Sesion) {
        almacen.guardar(sesion)
        estado.value = sesion
    }

    /** Publica una sesión ya guardada (recuperada al reabrir la aplicación). */
    fun restaurar(sesion: Sesion) {
        estado.value = sesion
    }

    fun actualizarPrivilegios(rol: String, privilegios: Set<String>) {
        val actual = estado.value ?: return
        if (actual.rol == rol && actual.privilegios == privilegios) return
        establecer(actual.copy(rol = rol, privilegios = privilegios))
    }

    /** Borra el token del almacenamiento seguro y deja la aplicación sin sesión. */
    fun limpiar() {
        almacen.borrar()
        estado.value = null
    }
}
