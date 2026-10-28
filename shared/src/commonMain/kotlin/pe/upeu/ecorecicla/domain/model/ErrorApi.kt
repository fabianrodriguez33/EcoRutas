package pe.upeu.ecorecicla.domain.model

import kotlin.coroutines.cancellation.CancellationException

/** Errores del dominio a los que la capa de datos traduce cada respuesta del servicio. */
sealed class ErrorApi(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa) {
    /** 400: validación fallida; [campos] asocia cada campo con su mensaje. */
    class Validacion(val campos: Map<String, String>, mensaje: String = "Datos inválidos") : ErrorApi(mensaje)

    /** 401: token ausente, alterado o vencido, o credenciales incorrectas en el inicio de sesión. */
    class NoAutorizado(mensaje: String = "Sesión no autorizada") : ErrorApi(mensaje)

    /** 403: autenticado pero sin el privilegio requerido. */
    class SinPrivilegio(mensaje: String = "La operación no está autorizada") : ErrorApi(mensaje)

    /** 404: el recurso no existe. */
    class NoEncontrado(mensaje: String = "El recurso no existe") : ErrorApi(mensaje)

    /** 409: regla de negocio del servidor incumplida; el mensaje es el motivo. */
    class Conflicto(mensaje: String) : ErrorApi(mensaje)

    /** 5xx: fallo del servidor. */
    class Servidor(mensaje: String = "Error del servidor") : ErrorApi(mensaje)

    class SinConexion(causa: Throwable? = null) : ErrorApi("Sin conexión con el servidor", causa)

    class TiempoAgotado(causa: Throwable? = null) : ErrorApi("El servidor tardó demasiado en responder", causa)

    class Desconocido(causa: Throwable? = null) : ErrorApi("Ocurrió un error inesperado", causa)

    /** Indica que el fallo es de conectividad: lo pendiente puede reintentarse más tarde. */
    val esDeConexion: Boolean get() = this is SinConexion || this is TiempoAgotado
}

/** Ejecuta [bloque] devolviendo un [Result]; deja pasar la cancelación y normaliza lo inesperado. */
suspend fun <T> capturarErrores(bloque: suspend () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ErrorApi) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(ErrorApi.Desconocido(e))
    }
