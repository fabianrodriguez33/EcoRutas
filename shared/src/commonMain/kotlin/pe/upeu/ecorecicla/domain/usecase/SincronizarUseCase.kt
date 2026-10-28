package pe.upeu.ecorecicla.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.ResumenSync
import pe.upeu.ecorecicla.domain.model.capturarErrores
import pe.upeu.ecorecicla.domain.repository.MonitorRed
import pe.upeu.ecorecicla.domain.repository.SincronizacionRepository

/**
 * Primero sube lo pendiente y después baja lo actualizado. Invertir el orden haría que la descarga
 * reemplazara filas locales aún no enviadas y se perdieran o se duplicaran al subir.
 */
class SincronizarUseCase(private val repositorio: SincronizacionRepository) {
    suspend operator fun invoke(): Result<ResumenSync> = capturarErrores {
        val envio = repositorio.enviarPendientes()
        val recibidos = repositorio.traerActualizaciones()
        ResumenSync(envio.enviados, envio.rechazados, recibidos)
    }
}

/** Emite true cada vez que el equipo tiene conexión; permite sincronizar al recuperarla. */
class ObservarConexionUseCase(private val monitor: MonitorRed) {
    operator fun invoke(): Flow<Boolean> = monitor.conectado
}
