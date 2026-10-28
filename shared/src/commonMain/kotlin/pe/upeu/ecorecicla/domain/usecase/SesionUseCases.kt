package pe.upeu.ecorecicla.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.model.capturarErrores
import pe.upeu.ecorecicla.domain.repository.SesionRepository

class ObservarSesionUseCase(private val repositorio: SesionRepository) {
    operator fun invoke(): StateFlow<Sesion?> = repositorio.sesion
}

class IniciarSesionUseCase(private val repositorio: SesionRepository) {
    suspend operator fun invoke(usuario: String, contrasena: String): Result<Sesion> {
        val campos = buildMap {
            if (usuario.isBlank()) put("usuario", "Ingrese su usuario")
            if (contrasena.isBlank()) put("contrasena", "Ingrese su contraseña")
        }
        if (campos.isNotEmpty()) return Result.failure(ErrorApi.Validacion(campos))
        return capturarErrores { repositorio.iniciarSesion(usuario.trim(), contrasena) }
    }
}

class RestaurarSesionUseCase(private val repositorio: SesionRepository) {
    operator fun invoke(): Sesion? = repositorio.restaurarSesion()
}

class CerrarSesionUseCase(private val repositorio: SesionRepository) {
    suspend operator fun invoke(): Result<Unit> = capturarErrores { repositorio.cerrarSesion() }
}
