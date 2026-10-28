package pe.upeu.ecorecicla.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.domain.repository.Reloj
import pe.upeu.ecorecicla.data.local.SesionActual
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.ResultadoRegistro
import pe.upeu.ecorecicla.domain.repository.PesajeRepository
import kotlin.math.round

/**
 * Offline-first: toda escritura se guarda primero en la base local como pendiente y después se intenta enviar.
 * Sin conexión la fila queda pendiente; si el servicio la rechaza se restaura el estado anterior y se informa el motivo.
 */
class PesajeRepositoryImpl(
    private val base: BaseLocal,
    private val enviador: Enviador,
    private val sesionActual: SesionActual,
    private val reloj: Reloj
) : PesajeRepository {

    override fun observarPesajes(puntoId: Long): Flow<List<Pesaje>> = base.observarPesajesDePunto(puntoId)

    override fun observarPesaje(id: Long): Flow<Pesaje?> = base.observarPesaje(id)

    override fun observarPendientes(): Flow<Int> = base.observarPendientes()

    override suspend fun registrar(
        puntoId: Long, rutaId: Long, material: String, pesoBruto: Double, tara: Double, observacion: String?,
        humedadEstimada: Double?
    ): ResultadoRegistro {
        val sesion = sesionActual.sesion.value ?: throw ErrorApi.NoAutorizado()
        val id = withContext(Dispatchers.Default) {
            val provisional = base.siguienteIdLocal()
            base.insertarPesaje(
                Pesaje(
                    id = provisional, puntoId = puntoId, fechaHora = reloj.ahoraIso(), material = material,
                    pesoBruto = pesoBruto, tara = tara, pesoNeto = neto(pesoBruto, tara), operario = sesion.usuario,
                    estado = EstadoPesaje.Registrado, pendiente = true, rutaId = rutaId, observacion = observacion,
                    humedadEstimada = humedadEstimada
                )
            )
            provisional
        }
        return enviarOReponer(id, previo = null)
    }

    override suspend fun corregir(
        id: Long, material: String, pesoBruto: Double, tara: Double, observacion: String?,
        humedadEstimada: Double?
    ): ResultadoRegistro {
        val previo = withContext(Dispatchers.Default) { base.pesajePorId(id) }
            ?: throw ErrorApi.NoEncontrado("El pesaje ya no existe")
        if (previo.estado is EstadoPesaje.Anulado) throw ErrorApi.Conflicto("El pesaje está anulado y no se puede corregir")
        withContext(Dispatchers.Default) {
            base.insertarPesaje(
                previo.copy(
                    material = material, pesoBruto = pesoBruto, tara = tara, pesoNeto = neto(pesoBruto, tara),
                    observacion = observacion, humedadEstimada = humedadEstimada, pendiente = true, errorEnvio = null
                )
            )
        }
        return enviarOReponer(id, previo)
    }

    override suspend fun anular(id: Long, motivo: String): ResultadoRegistro {
        val previo = withContext(Dispatchers.Default) { base.pesajePorId(id) }
            ?: throw ErrorApi.NoEncontrado("El pesaje ya no existe")
        if (previo.estado is EstadoPesaje.Anulado) throw ErrorApi.Conflicto("El pesaje ya está anulado")
        val anulado = previo.copy(estado = EstadoPesaje.Anulado(motivo), pendiente = true, errorEnvio = null)
        if (previo.id < 0) {
            // Nunca llegó al servidor: basta con descartar la fila local.
            withContext(Dispatchers.Default) { base.eliminarPesaje(previo.id) }
            return ResultadoRegistro(anulado.copy(pendiente = false), enviado = true)
        }
        withContext(Dispatchers.Default) { base.insertarPesaje(anulado) }
        return enviarOReponer(id, previo)
    }

    private suspend fun enviarOReponer(id: Long, previo: Pesaje?): ResultadoRegistro {
        try {
            val definitivo = enviador.enviar(id)
            if (definitivo != null) return ResultadoRegistro(definitivo, enviado = true)
        } catch (e: ErrorApi) {
            return withContext(Dispatchers.Default) {
                when {
                    // Sin conexión o 401: la fila queda pendiente (un 401 lo atiende la sesión global).
                    e.esDeConexion -> ResultadoRegistro(base.pesajePorId(id)!!, enviado = false)
                    e is ErrorApi.NoAutorizado -> throw e
                    e is ErrorApi.Servidor -> {
                        base.marcarErrorEnvio(id, e.message)
                        ResultadoRegistro(base.pesajePorId(id)!!, enviado = false)
                    }
                    else -> {
                        // El servicio lo rechazó: no se conserva el cambio local.
                        if (previo == null || e is ErrorApi.NoEncontrado) base.eliminarPesaje(id) else base.insertarPesaje(previo)
                        throw e
                    }
                }
            }
        }
        return ResultadoRegistro(withContext(Dispatchers.Default) { base.pesajePorId(id) }!!, enviado = true)
    }

    private fun neto(bruto: Double, tara: Double): Double = round((bruto - tara) * 100.0) / 100.0
}
