@file:OptIn(ExperimentalMaterial3Api::class)

package pe.upeu.ecorecicla.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.presentation.navigation.Navegador

@Composable
fun FormularioScreen(
    puntoId: Long,
    rutaId: Long,
    pesajeId: Long?,
    viewModel: FormularioViewModel = koinViewModel(),
    navegador: Navegador = koinInject()
) {
    LaunchedEffect(puntoId, rutaId, pesajeId) { viewModel.iniciar(puntoId, rutaId, pesajeId) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.terminado) {
        if (estado.terminado) navegador.atras()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) "Corregir pesaje" else "Registrar pesaje") },
                navigationIcon = { TextButton(onClick = { navegador.atras() }) { Text("Atrás") } }
            )
        }
    ) { relleno ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(relleno), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(relleno).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            estado.aviso?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
            }
            Campo("Material", estado.material, estado.errores["material"], !estado.guardando, viewModel::cambiarMaterial)
            Campo(
                "Peso bruto (kg)", estado.pesoBruto, estado.errores["pesoBruto"], !estado.guardando,
                viewModel::cambiarPesoBruto, KeyboardType.Decimal
            )
            Campo("Tara (kg)", estado.tara, estado.errores["tara"], !estado.guardando, viewModel::cambiarTara, KeyboardType.Decimal)
            Campo(
                "Observación (opcional)", estado.observacion, estado.errores["observacion"], !estado.guardando,
                viewModel::cambiarObservacion, singleLine = false
            )
            Text(
                "El peso neto lo calcula el servicio; sin conexión se guarda una estimación.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = viewModel::guardar, enabled = !estado.guardando, modifier = Modifier.fillMaxWidth()) {
                if (estado.guardando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (estado.esEdicion) "Guardar corrección" else "Guardar pesaje")
                }
            }
        }
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    error: String?,
    habilitado: Boolean,
    alCambiar: (String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        enabled = habilitado,
        singleLine = singleLine,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}
