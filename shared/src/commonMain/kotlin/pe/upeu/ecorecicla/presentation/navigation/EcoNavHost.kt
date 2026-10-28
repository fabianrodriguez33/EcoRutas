package pe.upeu.ecorecicla.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.presentation.detalle.DetalleScreen
import pe.upeu.ecorecicla.presentation.detalle.FormularioScreen
import pe.upeu.ecorecicla.presentation.listado.RutaScreen
import pe.upeu.ecorecicla.presentation.perfil.PerfilScreen
import pe.upeu.ecorecicla.presentation.sesion.LoginScreen

/** Dibuja la pantalla que está en la cima de la pila del [Navegador]. */
@Composable
fun EcoNavHost(navegador: Navegador = koinInject()) {
    // Crear la raíz decide si se entra directo o se pide el inicio de sesión.
    val raiz: RaizViewModel = koinViewModel()
    val listo by raiz.listo.collectAsStateWithLifecycle()
    if (!listo) return

    val pila by navegador.destino.collectAsStateWithLifecycle()
    when (val destino = pila.last()) {
        Destino.Login -> LoginScreen()
        Destino.Ruta -> RutaScreen()
        is Destino.Detalle -> DetalleScreen(destino.puntoId)
        is Destino.Formulario -> FormularioScreen(destino.puntoId, destino.rutaId, destino.pesajeId)
        Destino.Perfil -> PerfilScreen()
    }
}
