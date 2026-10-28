package pe.upeu.ecorecicla.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.upeu.ecorecicla.data.local.SesionActual

val JsonApi = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

class ConfigAutorizacion {
    var token: () -> String? = { null }
}

/** Único punto donde se añade la cabecera Authorization: ninguna función del servicio la toca. */
val PluginAutorizacion = createClientPlugin("AutorizacionJwt", ::ConfigAutorizacion) {
    val proveedor = pluginConfig.token
    onRequest { solicitud, _ ->
        proveedor()?.let { solicitud.header(HttpHeaders.Authorization, "Bearer $it") }
    }
}

/** Crea la única instancia de HttpClient de la aplicación; se inyecta con Koin. */
fun crearClienteHttp(motor: HttpClientEngine, config: ApiConfig, sesionActual: SesionActual): HttpClient =
    HttpClient(motor) {
        expectSuccess = false
        install(ContentNegotiation) { json(JsonApi) }
        install(HttpTimeout) {
            connectTimeoutMillis = config.tiempoConexionMs
            requestTimeoutMillis = config.tiempoPeticionMs
            socketTimeoutMillis = config.tiempoPeticionMs
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("EcoHttp: $message")
                }
            }
            // En producción no se registra nada; en depuración solo línea de petición y respuesta, sin cuerpos.
            level = if (config.depuracion) LogLevel.INFO else LogLevel.NONE
            sanitizeHeader { it == HttpHeaders.Authorization }
        }
        install(PluginAutorizacion) { token = { sesionActual.token() } }
        defaultRequest {
            url(config.urlBase)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }
