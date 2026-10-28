package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.Flow

/** Informa si el equipo tiene conexión; la implementación depende de la plataforma. */
interface MonitorRed {
    val conectado: Flow<Boolean>
}
