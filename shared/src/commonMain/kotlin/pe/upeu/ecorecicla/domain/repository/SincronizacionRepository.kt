package pe.upeu.ecorecicla.domain.repository

import pe.upeu.ecorecicla.domain.model.ResultadoEnvio

interface SincronizacionRepository {
    /** Sube lo pendiente: altas, correcciones y anulaciones hechas sin conexión. */
    suspend fun enviarPendientes(): ResultadoEnvio

    /** Trae del servicio la ruta del día, sus puntos y los pesajes; devuelve cuántos pesajes recibió. */
    suspend fun traerActualizaciones(): Int
}
