package pe.upeu.ecorecicla.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RutaInfo
import pe.upeu.ecorecicla.domain.model.capturarErrores
import pe.upeu.ecorecicla.domain.repository.RutaRepository

class ObservarRutaUseCase(private val repositorio: RutaRepository) {
    operator fun invoke(): Flow<RutaInfo?> = repositorio.observarRuta()
}

class ObservarPuntosUseCase(private val repositorio: RutaRepository) {
    operator fun invoke(texto: String = ""): Flow<List<PuntoAcopio>> = repositorio.observarPuntos(texto.trim())
}

class ObservarPuntoUseCase(private val repositorio: RutaRepository) {
    operator fun invoke(id: Long): Flow<PuntoAcopio?> = repositorio.observarPunto(id)
}

class CerrarRutaUseCase(private val repositorio: RutaRepository) {
    suspend operator fun invoke(): Result<Unit> = capturarErrores { repositorio.cerrarRuta() }
}
