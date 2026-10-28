package pe.upeu.ecorecicla.data.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Lee la fecha de vencimiento del token. La firma solo la verifica el servicio; el cliente la usa
 * para decidir si al reabrir la aplicación conviene entrar directamente o pedir el inicio de sesión.
 */
object TokenJwt {
    @OptIn(ExperimentalEncodingApi::class)
    fun vencimientoSegundos(token: String): Long? {
        val partes = token.split('.')
        if (partes.size != 3) return null
        return try {
            val carga = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                .decode(partes[1]).decodeToString()
            Json.parseToJsonElement(carga).jsonObject["exp"]?.jsonPrimitive?.longOrNull
        } catch (e: Exception) {
            null
        }
    }

    fun estaVigente(token: String, ahoraMs: Long): Boolean {
        val exp = vencimientoSegundos(token) ?: return false
        return exp * 1000 > ahoraMs
    }
}
