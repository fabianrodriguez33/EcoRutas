@file:OptIn(ExperimentalMaterial3Api::class)

package pe.upeu.ecorecicla.presentation.detalle

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.presentation.navigation.Destino
import pe.upeu.ecorecicla.presentation.navigation.Navegador

@Composable
fun DetalleScreen(puntoId: Long, viewModel: DetalleViewModel = koinViewModel(), navegador: Navegador = koinInject()) {
    LaunchedEffect(puntoId) { viewModel.cargar(puntoId) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensajes = remember { SnackbarHostState() }
    var pesajeAAnular by remember { mutableStateOf<Pesaje?>(null) }

    LaunchedEffect(estado.aviso) {
        estado.aviso?.let {
            mensajes.showSnackbar(it)
            viewModel.descartarAviso()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado.punto?.razonSocial ?: "Punto de acopio") },
                navigationIcon = { TextButton(onClick = { navegador.atras() }) { Text("Atrás") } }
            )
        },
        snackbarHost = { SnackbarHost(mensajes) }
    ) { relleno ->
        val punto = estado.punto
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(relleno), Alignment.Center) { CircularProgressIndicator() }
            punto == null -> Box(Modifier.fillMaxSize().padding(relleno), Alignment.Center) {
                Text("El punto de acopio ya no está en la ruta")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(relleno),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(punto.razonSocial, style = MaterialTheme.typography.titleLarge)
                            Text(punto.direccion, style = MaterialTheme.typography.bodyMedium)
                            Text("Zona: ${punto.zona}", style = MaterialTheme.typography.bodyMedium)
                            Text("Material habitual: ${punto.materialHabitual}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Visita n.º ${punto.secuencia} · ${if (punto.atendido) "Pesado" else "Pendiente de pesar"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (estado.rutaCerrada) {
                                Spacer(Modifier.height(4.dp))
                                Text("La ruta está cerrada", color = MaterialTheme.colorScheme.error)
                            }
                            if (estado.puedeRegistrar) {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { navegador.ir(Destino.Formulario(punto.id, estado.rutaId)) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Registrar pesaje") }
                            }
                        }
                    }
                }
                item { Text("Pesajes", style = MaterialTheme.typography.titleMedium) }
                if (estado.pesajes.isEmpty()) {
                    item { Text("Todavía no hay pesajes en este punto", style = MaterialTheme.typography.bodyMedium) }
                }
                items(estado.pesajes, key = { it.id }) { pesaje ->
                    FilaPesaje(
                        pesaje = pesaje,
                        puedeCorregir = estado.puedeCorregir(pesaje) && pesaje.estado is EstadoPesaje.Registrado,
                        puedeAnular = estado.puedeAnular && pesaje.estado is EstadoPesaje.Registrado && !pesaje.pendiente,
                        trabajando = estado.trabajando,
                        alCorregir = { navegador.ir(Destino.Formulario(punto.id, estado.rutaId, pesaje.id)) },
                        alAnular = { pesajeAAnular = pesaje }
                    )
                }
            }
        }
    }

    pesajeAAnular?.let { pesaje ->
        DialogoAnular(
            alConfirmar = { motivo ->
                viewModel.anular(pesaje.id, motivo)
                pesajeAAnular = null
            },
            alCancelar = { pesajeAAnular = null }
        )
    }
}

@Composable
private fun FilaPesaje(
    pesaje: Pesaje,
    puedeCorregir: Boolean,
    puedeAnular: Boolean,
    trabajando: Boolean,
    alCorregir: () -> Unit,
    alAnular: () -> Unit
) {
    val anulado = pesaje.estado as? EstadoPesaje.Anulado
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (anulado != null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pesaje.material, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (pesaje.pendiente) Etiqueta("Pendiente de envío", MaterialTheme.colorScheme.tertiaryContainer)
                if (anulado != null) Etiqueta("Anulado", MaterialTheme.colorScheme.errorContainer)
            }
            Text(
                "Bruto ${formatear(pesaje.pesoBruto)} kg · Tara ${formatear(pesaje.tara)} kg · " +
                    "Neto ${formatear(pesaje.pesoNeto)} kg${if (pesaje.pendiente) " (estimado)" else ""}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${pesaje.fechaHora.replace('T', ' ')} · ${pesaje.operario}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            pesaje.humedadEstimada?.let {
                Text("Humedad estimada: ${formatear(it)} %", style = MaterialTheme.typography.bodySmall)
            }
            pesaje.observacion?.let { Text("Obs.: $it", style = MaterialTheme.typography.bodySmall) }
            anulado?.let { Text("Motivo: ${it.motivo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            pesaje.errorEnvio?.let {
                Text("No se pudo enviar: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (puedeCorregir || puedeAnular) {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (puedeCorregir) TextButton(onClick = alCorregir, enabled = !trabajando) { Text("Corregir") }
                    if (puedeAnular) TextButton(onClick = alAnular, enabled = !trabajando) { Text("Anular") }
                }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color, modifier = Modifier.padding(start = 4.dp)) {
        Text(texto, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DialogoAnular(alConfirmar: (String) -> Unit, alCancelar: () -> Unit) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Anular pesaje") },
        text = {
            OutlinedTextField(
                value = motivo,
                onValueChange = { motivo = it },
                label = { Text("Motivo de la anulación") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { alConfirmar(motivo) }, enabled = motivo.isNotBlank()) { Text("Anular") } },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}

internal fun formatear(valor: Double): String {
    val redondeado = kotlin.math.round(valor * 100.0) / 100.0
    return if (redondeado == redondeado.toLong().toDouble()) redondeado.toLong().toString() else redondeado.toString()
}
