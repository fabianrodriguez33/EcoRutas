@file:OptIn(ExperimentalMaterial3Api::class)

package pe.upeu.ecorecicla.presentation.listado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.presentation.navigation.Destino
import pe.upeu.ecorecicla.presentation.navigation.Navegador

@Composable
fun RutaScreen(viewModel: RutaViewModel = koinViewModel(), navegador: Navegador = koinInject()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensajes = remember { SnackbarHostState() }

    LaunchedEffect(estado.aviso) {
        estado.aviso?.let {
            mensajes.showSnackbar(it)
            viewModel.descartarAviso()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ruta del día")
                        estado.ruta?.let {
                            Text(
                                "${it.fecha} · ${if (it.cerrada) "Cerrada" else "Abierta"}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { navegador.ir(Destino.Perfil) }) { Text("Perfil") }
                }
            )
        },
        snackbarHost = { SnackbarHost(mensajes) }
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            BarraSincronizacion(estado, viewModel)
            when {
                estado.cargando -> Centrado { CircularProgressIndicator() }
                estado.errorSinDatos -> ErrorSinDatos(estado, viewModel)
                estado.sinRuta -> Centrado {
                    Text("No tiene una ruta asignada para hoy", style = MaterialTheme.typography.bodyLarge)
                }
                else -> ContenidoRuta(estado, viewModel) { navegador.ir(Destino.Detalle(it.id)) }
            }
        }
    }
}

@Composable
private fun BarraSincronizacion(estado: RutaUiState, viewModel: RutaViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Última sincronización: ${estado.ultimaSincronizacion ?: "nunca"}",
                    style = MaterialTheme.typography.bodySmall
                )
                estado.resumen?.let {
                    Text(
                        "Enviados: ${it.enviados} · Recibidos: ${it.recibidos}" +
                            if (it.rechazados > 0) " · Rechazados: ${it.rechazados}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = viewModel::sincronizarAhora, enabled = !estado.sincronizando) {
                Text(if (estado.sincronizando) "Sincronizando…" else "Sincronizar")
            }
        }
        if (estado.sincronizando) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (estado.ruta != null) {
            estado.errorMensaje?.let { mensaje ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$mensaje. Se muestran los datos guardados.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (estado.permiteReintento) TextButton(onClick = viewModel::reintentar) { Text("Reintentar") }
                }
            }
        }
        OutlinedTextField(
            value = estado.busqueda,
            onValueChange = viewModel::buscar,
            label = { Text("Buscar punto de acopio") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Switch(checked = estado.soloPendientes, onCheckedChange = viewModel::filtrarPendientes)
            Text("Solo pendientes de pesar", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ErrorSinDatos(estado: RutaUiState, viewModel: RutaViewModel) {
    Centrado {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(estado.errorMensaje.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            if (estado.permiteReintento) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::reintentar) { Text("Reintentar") }
            }
        }
    }
}

@Composable
private fun ContenidoRuta(estado: RutaUiState, viewModel: RutaViewModel, alAbrir: (PuntoAcopio) -> Unit) {
    if (estado.puntos.isEmpty()) {
        Centrado {
            Text(
                if (estado.busqueda.isBlank() && !estado.soloPendientes) "La ruta no tiene puntos de acopio" else "Ningún punto coincide con el filtro",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(estado.puntos, key = { it.id }) { punto -> FilaPunto(punto) { alAbrir(punto) } }
        if (estado.puedeCerrarRuta) {
            item {
                Button(
                    onClick = viewModel::cerrarLaRuta,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Cerrar ruta del día") }
            }
        }
    }
}

@Composable
private fun FilaPunto(punto: PuntoAcopio, alAbrir: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alAbrir),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "${punto.secuencia}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(punto.razonSocial, style = MaterialTheme.typography.titleMedium)
                Text(punto.direccion, style = MaterialTheme.typography.bodySmall)
                Text(
                    "${punto.zona} · ${punto.materialHabitual}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (punto.atendido) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    if (punto.atendido) "Pesado" else "Por pesar",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { contenido() }
}
