package pe.upeu.ecorecicla.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.usecase.CerrarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPendientesUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarSesionUseCase

data class PerfilUiState(val sesion: Sesion? = null, val pendientes: Int = 0)

class PerfilViewModel(
    observarSesion: ObservarSesionUseCase,
    observarPendientes: ObservarPendientesUseCase,
    private val cerrarSesion: CerrarSesionUseCase
) : ViewModel() {

    val estado: StateFlow<PerfilUiState> = combine(observarSesion(), observarPendientes()) { sesion, pendientes ->
        PerfilUiState(sesion, pendientes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState())

    /** Borra el token y los datos locales; la raíz vuelve al inicio de sesión al quedar la sesión en nulo. */
    fun salir() {
        viewModelScope.launch { cerrarSesion() }
    }
}
