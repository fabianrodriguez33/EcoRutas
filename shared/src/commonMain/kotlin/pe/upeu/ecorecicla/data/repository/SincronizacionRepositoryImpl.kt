package pe.upeu.ecorecicla.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.domain.repository.Reloj
import pe.upeu.ecorecicla.data.local.SesionActual
import pe.upeu.ecorecicla.data.remote.ServicioApi
import pe.upeu.ecorecicla.data.remote.dto.RutaDto
import pe.upeu.ecorecicla.data.remote.dto.aDominio
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.ResultadoEnvio
import pe.upeu.ecorecicla.domain.model.RutaInfo
import pe.upeu.ecorecicla.domain.repository.SincronizacionRepository

class SincronizacionRepositoryImpl(
    private val api: ServicioApi,
    private val base: BaseLocal,
    private val enviador: Enviador,
    private val sesionActual: SesionActual,
    private val reloj: Reloj
) : SincronizacionRepository {

    override suspend fun enviarPendientes(): ResultadoEnvio {
        var enviados = 0
        var rechazados = 0
        for (id in withContext(Dispatchers.Default) { base.idsPendientes() }) {
            try {
                if (enviador.enviar(id) != null) enviados++
            } catch (e: ErrorApi) {
                // Sin conexión o sesión vencida: se detiene y lo pendiente se conserva para reintentar.
                if (e.esDeConexion || e is ErrorApi.NoAutorizado) throw e
                rechazados++
                withContext(Dispatchers.Default) {
                    if (e is ErrorApi.NoEncontrado) base.eliminarPesaje(id) else base.marcarErrorEnvio(id, detalle(e))
                }
            }
        }
        return ResultadoEnvio(enviados, rechazados)
    }

    override suspend fun traerActualizaciones(): Int {
        val usuario = api.usuarioActual()
        sesionActual.actualizarPrivilegios(usuario.rol, usuario.privilegios.toSet())

        repeat(INTENTOS) { intento ->
            val generacion = enviador.exclusivo { it }
            val ruta = try {
                api.rutaDelDia()
            } catch (e: ErrorApi.NoEncontrado) {
                withContext(Dispatchers.Default) { base.limpiarRutaYSincronizados() }
                return 0
            }
            val pesajes = ruta.puntos.flatMap { api.todosLosPesajes(it.id) }.map { it.aDominio() }
            // Si un envío terminó mientras se descargaba, la copia pudo quedar vieja: se vuelve a descargar.
            val aplicado = enviador.exclusivo { actual ->
                if (actual == generacion || intento == INTENTOS - 1) {
                    aplicar(ruta, pesajes)
                    true
                } else {
                    false
                }
            }
            if (aplicado) return pesajes.size
        }
        return 0
    }

    private fun aplicar(ruta: RutaDto, pesajes: List<Pesaje>) {
        base.reemplazarRuta(
            RutaInfo(ruta.id, ruta.fecha, ruta.cerrada, reloj.ahoraMs()),
            ruta.puntos.map { it.aDominio(ruta.id) },
            pesajes
        )
    }

    private fun detalle(e: ErrorApi): String =
        if (e is ErrorApi.Validacion && e.campos.isNotEmpty()) e.campos.values.joinToString(". ") else e.message.orEmpty()

    private companion object {
        const val INTENTOS = 3
    }
}
