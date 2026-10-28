package pe.upeu.ecorecicla.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RutaInfo

/** Acceso a la base SQLDelight: las consultas de lectura devuelven flujos que se re-emiten al cambiar la tabla. */
class BaseLocal(private val db: EcoDatabase) {

    private val q get() = db.consultasQueries

    // ---- Lecturas observables -------------------------------------------------------------

    fun observarRuta(): Flow<RutaInfo?> =
        q.observarRuta(::aRutaInfo).asFlow().mapToOneOrNull(Dispatchers.Default)

    fun observarPuntos(texto: String): Flow<List<PuntoAcopio>> =
        q.observarPuntos(texto, ::aPunto).asFlow().mapToList(Dispatchers.Default)

    fun observarPunto(id: Long): Flow<PuntoAcopio?> =
        q.observarPunto(id, ::aPunto).asFlow().mapToOneOrNull(Dispatchers.Default)

    fun observarPesajesDePunto(puntoId: Long): Flow<List<Pesaje>> =
        q.observarPesajesDePunto(puntoId, ::aPesaje).asFlow().mapToList(Dispatchers.Default)

    fun observarPesaje(id: Long): Flow<Pesaje?> =
        q.observarPesaje(id, ::aPesaje).asFlow().mapToOneOrNull(Dispatchers.Default)

    fun observarPendientes(): Flow<Int> =
        q.contarPendientes().asFlow().mapToOne(Dispatchers.Default).map { it.toInt() }

    // ---- Lecturas puntuales ---------------------------------------------------------------

    fun rutaActual(): RutaInfo? = q.observarRuta(::aRutaInfo).executeAsOneOrNull()

    fun pesajePorId(id: Long): Pesaje? = q.pesajePorId(id, ::aPesaje).executeAsOneOrNull()

    fun idsPendientes(): List<Long> = q.idsPendientes().executeAsList()

    fun siguienteIdLocal(): Long = q.siguienteIdLocal().executeAsOne()

    // ---- Escrituras -----------------------------------------------------------------------

    fun <T> transaccion(bloque: () -> T): T = db.transactionWithResult { bloque() }

    fun insertarRuta(id: Long, fecha: String, cerrada: Boolean, ultimaSincronizacion: Long?) =
        q.insertarRuta(id, fecha, cerrada.aEntero(), ultimaSincronizacion)

    fun insertarPunto(p: PuntoAcopio) =
        q.insertarPunto(p.id, p.rutaId, p.razonSocial, p.direccion, p.zona, p.materialHabitual, p.secuencia.toLong(), p.atendido.aEntero())

    fun insertarPesaje(p: Pesaje) = q.insertarPesaje(
        p.id, p.puntoId, p.rutaId, p.fechaHora, p.material, p.pesoBruto, p.tara, p.pesoNeto, p.operario,
        (p.estado is EstadoPesaje.Anulado).aEntero(), (p.estado as? EstadoPesaje.Anulado)?.motivo, p.observacion,
        p.pendiente.aEntero(), p.errorEnvio, p.humedadEstimada
    )

    fun marcarRutaCerrada(id: Long) = q.marcarRutaCerrada(id)

    fun marcarErrorEnvio(id: Long, mensaje: String?) = q.marcarErrorEnvio(mensaje, id)

    fun eliminarPesaje(id: Long) = q.eliminarPesaje(id)

    /** Quita la ruta, sus puntos y los pesajes ya sincronizados; los pendientes se conservan. */
    fun limpiarRutaYSincronizados() = transaccion {
        q.eliminarPuntos()
        q.eliminarRutas()
        q.eliminarPesajesSincronizados()
    }

    fun reemplazarRuta(ruta: RutaInfo, puntos: List<PuntoAcopio>, pesajes: List<Pesaje>) = transaccion {
        q.eliminarPuntos()
        q.eliminarRutas()
        q.eliminarPesajesSincronizados()
        insertarRuta(ruta.id, ruta.fecha, ruta.cerrada, ruta.ultimaSincronizacion)
        puntos.forEach(::insertarPunto)
        val pendientes = idsPendientes().toSet()
        pesajes.filter { it.id !in pendientes }.forEach(::insertarPesaje)
    }

    /** Deja el equipo limpio: elimina toda la información del usuario anterior. */
    fun vaciar() = transaccion {
        q.eliminarPesajes()
        q.eliminarPuntos()
        q.eliminarRutas()
    }
}

// ---- Conversión de filas al dominio --------------------------------------------------------

private fun Boolean.aEntero(): Long = if (this) 1L else 0L

private fun aRutaInfo(id: Long, fecha: String, cerrada: Long, ultima: Long?) =
    RutaInfo(id, fecha, cerrada == 1L, ultima)

private fun aPunto(
    id: Long, rutaId: Long, razonSocial: String, direccion: String, zona: String, material: String,
    secuencia: Long, atendido: Long
) = PuntoAcopio(id, razonSocial, direccion, zona, material, secuencia.toInt(), atendido == 1L, rutaId)

private fun aPesaje(
    id: Long, puntoId: Long, rutaId: Long, fechaHora: String, material: String, bruto: Double, tara: Double,
    neto: Double, operario: String, anulado: Long, motivo: String?, observacion: String?,
    pendiente: Long, errorEnvio: String?, humedad: Double?
) = Pesaje(
    id = id, puntoId = puntoId, fechaHora = fechaHora, material = material, pesoBruto = bruto, tara = tara,
    pesoNeto = neto, operario = operario,
    estado = if (anulado == 1L) EstadoPesaje.Anulado(motivo ?: "Sin motivo") else EstadoPesaje.Registrado,
    pendiente = pendiente == 1L, rutaId = rutaId, observacion = observacion, errorEnvio = errorEnvio,
    humedadEstimada = humedad
)
