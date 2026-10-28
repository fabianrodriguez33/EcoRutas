package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.StateFlow
import pe.upeu.ecorecicla.domain.model.Sesion

interface SesionRepository {
    /** Sesión vigente; pasa a nulo al cerrar sesión o al recibir un 401. */
    val sesion: StateFlow<Sesion?>

    suspend fun iniciarSesion(usuario: String, contrasena: String): Sesion

    /** Recupera la sesión guardada si su token sigue vigente; si venció, limpia el almacenamiento. */
    fun restaurarSesion(): Sesion?

    /** Borra el token y los datos locales para dejar el equipo limpio para el turno siguiente. */
    suspend fun cerrarSesion()
}
