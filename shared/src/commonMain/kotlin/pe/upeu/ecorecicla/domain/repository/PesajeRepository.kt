package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.ResultadoRegistro

interface PesajeRepository {
    fun observarPesajes(puntoId: Long): Flow<List<Pesaje>>

    fun observarPesaje(id: Long): Flow<Pesaje?>

    fun observarPendientes(): Flow<Int>

    /** Guarda el pesaje en la base local como pendiente y lo envía si hay conexión. */
    suspend fun registrar(
        puntoId: Long, rutaId: Long, material: String, pesoBruto: Double, tara: Double, observacion: String?,
        humedadEstimada: Double? = null
    ): ResultadoRegistro

    suspend fun corregir(
        id: Long, material: String, pesoBruto: Double, tara: Double, observacion: String?,
        humedadEstimada: Double? = null
    ): ResultadoRegistro

    suspend fun anular(id: Long, motivo: String): ResultadoRegistro
}
