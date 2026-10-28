package pe.upeu.ecorecicla.data.remote

import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.SerializationException
import pe.upeu.ecorecicla.data.remote.dto.ErrorDto
import pe.upeu.ecorecicla.domain.model.ErrorApi
import kotlin.coroutines.cancellation.CancellationException

/** Traduce en un solo lugar cada respuesta de error del servicio a la jerarquía de errores del dominio. */
suspend fun HttpResponse.aErrorApi(): ErrorApi {
    val dto = try {
        body<ErrorDto>()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
    val mensaje = dto?.message
    return when (status.value) {
        400 -> ErrorApi.Validacion(dto?.validationErrors.orEmpty(), mensaje ?: "Datos inválidos")
        401 -> ErrorApi.NoAutorizado(mensaje ?: "Sesión no autorizada")
        403 -> ErrorApi.SinPrivilegio(mensaje ?: "La operación no está autorizada")
        404 -> ErrorApi.NoEncontrado(mensaje ?: "El recurso no existe")
        409 -> ErrorApi.Conflicto(mensaje ?: "La operación no cumple una regla del servidor")
        in 500..599 -> ErrorApi.Servidor(mensaje ?: "Error del servidor")
        else -> ErrorApi.Desconocido()
    }
}

/** Traduce las excepciones de transporte: tiempo agotado, serialización o falta de conexión. */
fun Throwable.aErrorApi(): ErrorApi = when (this) {
    is ErrorApi -> this
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> ErrorApi.TiempoAgotado(this)
    is SerializationException -> ErrorApi.Desconocido(this)
    else -> ErrorApi.SinConexion(this)
}
