package pe.upeu.ecorecicla.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.usecase.IniciarSesionUseCase
import pe.upeu.ecorecicla.presentation.comoErrorApi
import pe.upeu.ecorecicla.presentation.mensajeUsuario

data class LoginUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null
)

class LoginViewModel(private val iniciarSesion: IniciarSesionUseCase) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun cambiarUsuario(valor: String) = _estado.update { it.copy(usuario = valor, errores = it.errores - "usuario", mensaje = null) }

    fun cambiarContrasena(valor: String) =
        _estado.update { it.copy(contrasena = valor, errores = it.errores - "contrasena", mensaje = null) }

    fun ingresar() {
        val actual = _estado.value
        if (actual.cargando) return
        _estado.update { it.copy(cargando = true, errores = emptyMap(), mensaje = null) }
        viewModelScope.launch {
            iniciarSesion(actual.usuario, actual.contrasena).fold(
                onSuccess = { _estado.update { it.copy(cargando = false, contrasena = "") } },
                onFailure = { fallo ->
                    val error = fallo.comoErrorApi()
                    _estado.update {
                        it.copy(
                            cargando = false,
                            errores = (error as? ErrorApi.Validacion)?.campos.orEmpty(),
                            // En el inicio de sesión un 401 significa credenciales incorrectas, no sesión vencida.
                            mensaje = when (error) {
                                is ErrorApi.NoAutorizado -> "Usuario o contraseña incorrectos"
                                is ErrorApi.Validacion -> null
                                else -> error.mensajeUsuario()
                            }
                        )
                    }
                }
            )
        }
    }
}
