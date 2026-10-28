package pe.upeu.ecorecicla.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.domain.repository.Reloj
import pe.upeu.ecorecicla.data.local.SesionActual
import pe.upeu.ecorecicla.data.local.TokenJwt
import pe.upeu.ecorecicla.data.remote.ServicioApi
import pe.upeu.ecorecicla.data.remote.dto.aDominio
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.SesionRepository

class SesionRepositoryImpl(
    private val api: ServicioApi,
    private val sesionActual: SesionActual,
    private val almacen: AlmacenSeguro,
    private val base: BaseLocal,
    private val reloj: Reloj
) : SesionRepository {

    override val sesion: StateFlow<Sesion?> = sesionActual.sesion

    override suspend fun iniciarSesion(usuario: String, contrasena: String): Sesion {
        val nueva = api.login(usuario, contrasena).aDominio()
        withContext(Dispatchers.Default) {
            // Equipo compartido: los datos locales de otro usuario no se heredan.
            val propietario = almacen.propietarioDatos()
            if (propietario != null && propietario != nueva.usuario) base.vaciar()
            almacen.fijarPropietarioDatos(nueva.usuario)
        }
        sesionActual.establecer(nueva)
        return nueva
    }

    override fun restaurarSesion(): Sesion? {
        val guardada = almacen.leer() ?: return null
        if (!TokenJwt.estaVigente(guardada.token, reloj.ahoraMs())) {
            sesionActual.limpiar()
            return null
        }
        sesionActual.restaurar(guardada)
        return guardada
    }

    override suspend fun cerrarSesion() {
        sesionActual.limpiar()
        withContext(Dispatchers.Default) {
            base.vaciar()
            almacen.fijarPropietarioDatos(null)
        }
    }
}
