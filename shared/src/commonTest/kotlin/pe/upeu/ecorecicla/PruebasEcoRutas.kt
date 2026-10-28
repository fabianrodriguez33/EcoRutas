package pe.upeu.ecorecicla

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import pe.upeu.ecorecicla.data.local.TokenJwt
import pe.upeu.ecorecicla.data.remote.JsonApi
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.aDominio
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val PESAJE_JSON = """{"id":42,"puntoId":3,"rutaId":1,"fechaHora":"2026-10-28T08:15:00","material":"Carton",
"pesoBruto":50.5,"tara":4.5,"pesoNeto":46.0,"operario":"operario1","anulado":true,"motivoAnulacion":"Error de peso",
"observacion":null,"campoNuevoDelServidor":"se ignora"}"""

private const val RUTA_JSON = """{"id":1,"fecha":"2026-10-28","estado":"ABIERTA","puntos":[
{"id":3,"razonSocial":"Clinica San Gabriel","direccion":"Av. Salud 780","zona":"Centro","materialHabitual":"Papel",
"secuencia":1,"atendido":false,"motivoOmision":null}]}"""

private const val PAGINA_VACIA = """{"contenido":[],"pagina":0,"tamanio":100,"totalElementos":0,"totalPaginas":0,"ultima":true}"""

private const val ME_JSON = """{"id":1,"usuario":"operario1","nombre":"Luis","rol":"OPERARIO","privilegios":["PESAJE_CREAR","PUNTO_LEER"]}"""

class PruebasEcoRutas {

    @Test
    fun conversionDeDtoAlDominio() {
        // El JSON usa "anulado" y trae una clave desconocida: se mapea por nombre serializado y se tolera lo extra.
        val dto = JsonApi.decodeFromString<PesajeDto>(PESAJE_JSON)
        val pesaje: Pesaje = dto.aDominio()
        assertEquals(42L, pesaje.id)
        assertEquals(EstadoPesaje.Anulado("Error de peso"), pesaje.estado)
        assertFalse(pesaje.pendiente)
        assertEquals(46.0, pesaje.pesoNeto)
        assertNull(pesaje.observacion)
    }

    @Test
    fun traduccionDeRespuestasDeErrorAlDominio() = runTest {
        var codigo = HttpStatusCode.Conflict
        var cuerpo = """{"status":409,"message":"La ruta ya esta cerrada","validationErrors":{}}"""
        val escenario = Escenario { json(cuerpo, codigo) }
        escenario.iniciarSesionComoOperario()

        val conflicto = assertFailsWith<ErrorApi.Conflicto> { escenario.api.rutaDelDia() }
        assertEquals("La ruta ya esta cerrada", conflicto.message)

        codigo = HttpStatusCode.BadRequest
        cuerpo = """{"status":400,"message":"Datos invalidos","validationErrors":{"pesoBruto":"debe ser mayor que la tara"}}"""
        val validacion = assertFailsWith<ErrorApi.Validacion> { escenario.api.rutaDelDia() }
        assertEquals("debe ser mayor que la tara", validacion.campos["pesoBruto"])

        codigo = HttpStatusCode.Forbidden
        assertFailsWith<ErrorApi.SinPrivilegio> { escenario.api.rutaDelDia() }
        assertNotNull(escenario.sesionActual.sesion.value, "un 403 no cierra la sesión")

        codigo = HttpStatusCode.NotFound
        assertFailsWith<ErrorApi.NoEncontrado> { escenario.api.rutaDelDia() }

        codigo = HttpStatusCode.InternalServerError
        assertFailsWith<ErrorApi.Servidor> { escenario.api.rutaDelDia() }

        codigo = HttpStatusCode.Unauthorized
        assertFailsWith<ErrorApi.NoAutorizado> { escenario.api.rutaDelDia() }
        assertNull(escenario.sesionActual.sesion.value, "un 401 borra el token y limpia la sesión")
        assertNull(escenario.almacen.sesion)
    }

    @Test
    fun registroSinConexionQuedaPendienteConIdProvisional() = runTest {
        val escenario = Escenario { throw RuntimeException("sin red") }
        escenario.iniciarSesionComoOperario()
        assertEquals(-1L, escenario.base.siguienteIdLocal(), "con la tabla vacía el primer id provisional es -1")

        val primero = escenario.pesajes.registrar(3, 1, "Papel", 50.0, 5.0, null)
        val segundo = escenario.pesajes.registrar(3, 1, "Carton", 20.0, 2.0, "obs")

        assertFalse(primero.enviado)
        assertTrue(primero.pesaje.pendiente)
        assertEquals(-1L, primero.pesaje.id)
        assertEquals(-2L, segundo.pesaje.id)
        assertEquals(45.0, primero.pesaje.pesoNeto)
        assertEquals(listOf(-1L, -2L).toSet(), escenario.base.idsPendientes().toSet())
        assertEquals(-3L, escenario.base.siguienteIdLocal())
    }

    @Test
    fun sincronizacionSubePendientesAntesDeBajarYNoDuplica() = runTest {
        val escenario = Escenario { solicitud ->
            val ruta = solicitud.url.encodedPath
            when {
                solicitud.method.value == "POST" && ruta.endsWith("/pesajes") -> json(
                    """{"id":99,"puntoId":3,"rutaId":1,"fechaHora":"2026-10-28T09:00:01","material":"Papel",
                    "pesoBruto":50.0,"tara":5.0,"pesoNeto":45.0,"operario":"operario1","anulado":false}""",
                    HttpStatusCode.Created
                )
                ruta.endsWith("/auth/me") -> json(ME_JSON)
                ruta.endsWith("/rutas/dia") -> json(RUTA_JSON)
                ruta.endsWith("/puntos/3/pesajes") -> json(
                    """{"contenido":[{"id":99,"puntoId":3,"rutaId":1,"fechaHora":"2026-10-28T09:00:01","material":"Papel",
                    "pesoBruto":50.0,"tara":5.0,"pesoNeto":45.0,"operario":"operario1","anulado":false}],
                    "pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}"""
                )
                else -> json(PAGINA_VACIA)
            }
        }
        escenario.iniciarSesionComoOperario()
        // Un registro hecho sin conexión: fila local con id negativo y marca de pendiente.
        escenario.base.insertarPesaje(
            Pesaje(-1, 3, "2026-10-28T09:00:00", "Papel", 50.0, 5.0, 45.0, "operario1", EstadoPesaje.Registrado, true, rutaId = 1)
        )

        val resumen = SincronizarUseCase(escenario.sincronizacion)().getOrThrow()

        val primeraSubida = escenario.peticiones.indexOf("POST /api/v1/pesajes")
        val primeraBajada = escenario.peticiones.indexOf("GET /api/v1/rutas/dia")
        assertTrue(primeraSubida >= 0 && primeraBajada > primeraSubida, "primero se sube y después se baja: ${escenario.peticiones}")
        assertEquals(1, resumen.enviados)
        assertEquals(1, resumen.recibidos)
        assertTrue(escenario.base.idsPendientes().isEmpty())
        assertNull(escenario.base.pesajePorId(-1), "la fila provisional se sustituyó")
        assertEquals(false, escenario.base.pesajePorId(99)?.pendiente, "la definitiva quedó una sola vez")
    }

    @Test
    fun elContadorDePendientesSaleDeLaConsultaObservable() = runTest {
        val escenario = Escenario { throw RuntimeException("sin red") }
        escenario.iniciarSesionComoOperario()
        assertEquals(0, escenario.base.observarPendientes().first())
        escenario.pesajes.registrar(3, 1, "Papel", 50.0, 5.0, null)
        escenario.pesajes.registrar(3, 1, "Carton", 20.0, 2.0, null)
        assertEquals(2, escenario.base.observarPendientes().first())
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun tokenVigenteSeReconoceYVencidoNo() {
        fun token(exp: Long): String {
            val carga = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode("""{"sub":"1","exp":$exp}""".encodeToByteArray())
            return "cabecera.$carga.firma"
        }
        assertTrue(TokenJwt.estaVigente(token(2_000_000_000L), ahoraMs = 1_800_000_000_000L))
        assertFalse(TokenJwt.estaVigente(token(1_700_000_000L), ahoraMs = 1_800_000_000_000L))
        assertFalse(TokenJwt.estaVigente("no-es-un-token", ahoraMs = 0))
    }
}
