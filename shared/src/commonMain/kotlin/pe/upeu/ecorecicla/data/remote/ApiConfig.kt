package pe.upeu.ecorecicla.data.remote

/** Configuración centralizada del cliente: dirección base, tiempos de espera y nivel de registro. */
class ApiConfig(
    val urlBase: String = URL_EMULADOR,
    val depuracion: Boolean = false,
    val tiempoConexionMs: Long = 5_000,
    val tiempoPeticionMs: Long = 15_000
) {
    companion object {
        /** El emulador de Android expone el equipo anfitrión en 10.0.2.2. Para un teléfono físico use la IP de la PC. */
        const val URL_EMULADOR = "http://10.0.2.2:8080/api/v1/"
    }
}
