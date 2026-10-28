package pe.upeu.ecorecicla.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.remote.ServicioApi
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RutaInfo
import pe.upeu.ecorecicla.domain.repository.RutaRepository

class RutaRepositoryImpl(private val api: ServicioApi, private val base: BaseLocal) : RutaRepository {

    override fun observarRuta(): Flow<RutaInfo?> = base.observarRuta()

    override fun observarPuntos(texto: String): Flow<List<PuntoAcopio>> = base.observarPuntos(texto)

    override fun observarPunto(id: Long): Flow<PuntoAcopio?> = base.observarPunto(id)

    override suspend fun cerrarRuta() {
        val ruta = withContext(Dispatchers.Default) { base.rutaActual() }
            ?: throw ErrorApi.NoEncontrado("No hay una ruta cargada para cerrar")
        api.cerrarRuta(ruta.id)
        withContext(Dispatchers.Default) { base.marcarRutaCerrada(ruta.id) }
    }
}
