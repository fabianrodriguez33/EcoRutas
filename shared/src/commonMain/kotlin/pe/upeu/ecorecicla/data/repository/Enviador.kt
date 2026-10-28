package pe.upeu.ecorecicla.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.remote.ServicioApi
import pe.upeu.ecorecicla.data.remote.dto.PesajeCrearDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeEditarDto
import pe.upeu.ecorecicla.data.remote.dto.aDominio
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje

/**
 * Envía al servicio una fila pendiente. Es el único camino de subida: lo usan el registro inmediato y
 * la sincronización. El cerrojo y la relectura de la fila evitan que dos envíos simultáneos dupliquen una fila.
 */
class Enviador(private val api: ServicioApi, private val base: BaseLocal) {

    private val cerrojo = Mutex()
    private var generacion = 0

    /**
     * Envía la fila [id] si sigue pendiente. Devuelve la fila definitiva, o nulo si ya no había nada que enviar.
     * Lanza [pe.upeu.ecorecicla.domain.model.ErrorApi] si el servicio la rechaza o no hay conexión.
     */
    suspend fun enviar(id: Long): Pesaje? = cerrojo.withLock {
        val fila = base.pesajePorId(id)?.takeIf { it.pendiente } ?: return@withLock null
        val anulado = fila.estado as? EstadoPesaje.Anulado
        val definitivo: Pesaje = when {
            fila.id < 0 -> api.crearPesaje(
                PesajeCrearDto(
                    fila.rutaId, fila.puntoId, fila.material, fila.pesoBruto, fila.tara, fila.observacion,
                    fila.humedadEstimada
                )
            ).aDominio()
            anulado != null -> {
                api.anularPesaje(fila.id, anulado.motivo)
                fila.copy(pendiente = false, errorEnvio = null)
            }
            else -> api.editarPesaje(
                fila.id, PesajeEditarDto(fila.material, fila.pesoBruto, fila.tara, fila.observacion, fila.humedadEstimada)
            ).aDominio()
        }
        // La fila provisional se sustituye por la definitiva dentro de la misma transacción.
        base.transaccion {
            base.eliminarPesaje(fila.id)
            base.insertarPesaje(definitivo)
        }
        generacion++
        definitivo
    }

    /** Ejecuta [bloque] sin envíos en curso; recibe la generación actual (cambia con cada envío exitoso). */
    suspend fun <T> exclusivo(bloque: (generacion: Int) -> T): T = cerrojo.withLock { bloque(generacion) }
}
