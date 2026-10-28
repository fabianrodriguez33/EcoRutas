package pe.upeu.ecorecicla

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.local.SesionActual
import pe.upeu.ecorecicla.data.remote.ApiConfig
import pe.upeu.ecorecicla.data.remote.ServicioApi
import pe.upeu.ecorecicla.data.remote.crearClienteHttp
import pe.upeu.ecorecicla.data.repository.Enviador
import pe.upeu.ecorecicla.data.repository.PesajeRepositoryImpl
import pe.upeu.ecorecicla.data.repository.SincronizacionRepositoryImpl
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.Reloj

/** Controlador SQLite en memoria con el esquema creado; cada plataforma de prueba lo aporta. */
expect fun crearControladorEnMemoria(): SqlDriver

typealias Manejador = suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData

fun MockRequestHandleScope.json(cuerpo: String, estado: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(cuerpo, estado, headersOf(HttpHeaders.ContentType, "application/json"))

class AlmacenFalso : AlmacenSeguro {
    var sesion: Sesion? = null
    var propietario: String? = null
    override fun guardar(sesion: Sesion) { this.sesion = sesion }
    override fun leer(): Sesion? = sesion
    override fun borrar() { sesion = null }
    override fun propietarioDatos(): String? = propietario
    override fun fijarPropietarioDatos(usuario: String?) { propietario = usuario }
}

class RelojFijo(var ahora: Long = 1_800_000_000_000L) : Reloj {
    override fun ahoraMs(): Long = ahora
    override fun ahoraIso(): String = "2026-10-28T09:00:00"
    override fun formatearHora(ms: Long): String = "09:00"
}

/** Arma la aplicación de datos completa con red simulada (MockEngine) y base en memoria. */
class Escenario(manejador: Manejador) {
    val peticiones = mutableListOf<String>()
    val base = BaseLocal(EcoDatabase(crearControladorEnMemoria()))
    val almacen = AlmacenFalso()
    val sesionActual = SesionActual(almacen)
    val reloj = RelojFijo()

    private val motor = MockEngine { solicitud ->
        peticiones += "${solicitud.method.value} ${solicitud.url.encodedPath}"
        manejador(this, solicitud)
    }
    val api = ServicioApi(crearClienteHttp(motor, ApiConfig("http://localhost/api/v1/"), sesionActual), sesionActual)
    val enviador = Enviador(api, base)
    val pesajes = PesajeRepositoryImpl(base, enviador, sesionActual, reloj)
    val sincronizacion = SincronizacionRepositoryImpl(api, base, enviador, sesionActual, reloj)

    fun iniciarSesionComoOperario() {
        sesionActual.establecer(
            Sesion("token", "operario1", "OPERARIO", setOf("PESAJE_CREAR", "PUNTO_LEER", "PESAJE_LEER"), "Luis")
        )
    }
}
