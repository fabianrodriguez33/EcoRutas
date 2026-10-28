package pe.upeu.ecorecicla.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import pe.upeu.ecorecicla.data.local.SesionActual
import pe.upeu.ecorecicla.data.remote.dto.LoginPeticionDto
import pe.upeu.ecorecicla.data.remote.dto.LoginRespuestaDto
import pe.upeu.ecorecicla.data.remote.dto.PaginaDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeCrearDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeEditarDto
import pe.upeu.ecorecicla.data.remote.dto.RutaDto
import pe.upeu.ecorecicla.data.remote.dto.UsuarioActualDto
import pe.upeu.ecorecicla.domain.model.ErrorApi
import kotlin.coroutines.cancellation.CancellationException

/** Llamadas al servicio EcoAPI. Todo error sale como [ErrorApi]; un 401 autenticado limpia la sesión. */
class ServicioApi(private val cliente: HttpClient, private val sesionActual: SesionActual) {

    suspend fun login(usuario: String, contrasena: String): LoginRespuestaDto =
        pedir(autenticado = false) { cliente.post("auth/login") { setBody(LoginPeticionDto(usuario, contrasena)) } }

    suspend fun usuarioActual(): UsuarioActualDto = pedir { cliente.get("auth/me") }

    suspend fun rutaDelDia(): RutaDto = pedir { cliente.get("rutas/dia") }

    suspend fun cerrarRuta(rutaId: Long): RutaDto = pedir { cliente.post("rutas/$rutaId/cerrar") }

    suspend fun pesajesDePunto(puntoId: Long, pagina: Int, tamanio: Int = TAMANIO_PAGINA): PaginaDto<PesajeDto> =
        pedir {
            cliente.get("puntos/$puntoId/pesajes") {
                parameter("pagina", pagina)
                parameter("tamanio", tamanio)
            }
        }

    /** Recorre todas las páginas de pesajes de un punto. */
    suspend fun todosLosPesajes(puntoId: Long): List<PesajeDto> {
        val resultado = mutableListOf<PesajeDto>()
        var pagina = 0
        do {
            val respuesta = pesajesDePunto(puntoId, pagina)
            resultado += respuesta.items
            pagina++
        } while (!respuesta.ultima)
        return resultado
    }

    suspend fun crearPesaje(peticion: PesajeCrearDto): PesajeDto =
        pedir { cliente.post("pesajes") { setBody(peticion) } }

    suspend fun editarPesaje(id: Long, peticion: PesajeEditarDto): PesajeDto =
        pedir { cliente.put("pesajes/$id") { setBody(peticion) } }

    /** 204 No Content: solo se comprueba el código, no se intenta convertir un cuerpo. */
    suspend fun anularPesaje(id: Long, motivo: String) {
        verificar(true) { cliente.delete("pesajes/$id") { parameter("motivo", motivo) } }
    }

    private suspend fun verificar(autenticado: Boolean, llamada: suspend () -> HttpResponse): HttpResponse {
        val respuesta = try {
            llamada()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.aErrorApi()
        }
        if (!respuesta.status.isSuccess()) {
            val error = respuesta.aErrorApi()
            if (autenticado && error is ErrorApi.NoAutorizado) sesionActual.limpiar()
            throw error
        }
        return respuesta
    }

    private suspend inline fun <reified T> pedir(
        autenticado: Boolean = true,
        noinline llamada: suspend () -> HttpResponse
    ): T {
        val respuesta = verificar(autenticado, llamada)
        return try {
            respuesta.body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ErrorApi.Desconocido(e)
        }
    }

    companion object {
        const val TAMANIO_PAGINA = 100
    }
}
