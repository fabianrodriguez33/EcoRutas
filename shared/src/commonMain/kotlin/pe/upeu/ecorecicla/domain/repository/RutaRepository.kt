package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RutaInfo

interface RutaRepository {
    fun observarRuta(): Flow<RutaInfo?>

    /** Puntos de la ruta en orden de visita, leídos de la base local y filtrados por [texto]. */
    fun observarPuntos(texto: String = ""): Flow<List<PuntoAcopio>>

    fun observarPunto(id: Long): Flow<PuntoAcopio?>

    /** Cierra la ruta en el servicio (requiere conexión) y refleja el estado en la base local. */
    suspend fun cerrarRuta()
}
