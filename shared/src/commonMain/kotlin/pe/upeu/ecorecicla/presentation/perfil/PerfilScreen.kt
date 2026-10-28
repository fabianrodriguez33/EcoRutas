@file:OptIn(ExperimentalMaterial3Api::class)

package pe.upeu.ecorecicla.presentation.perfil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.presentation.navigation.Navegador

@Composable
fun PerfilScreen(viewModel: PerfilViewModel = koinViewModel(), navegador: Navegador = koinInject()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var confirmando by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = { TextButton(onClick = { navegador.atras() }) { Text("Atrás") } }
            )
        }
    ) { relleno ->
        val sesion = estado.sesion
        Column(Modifier.fillMaxSize().padding(relleno).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (sesion != null) {
                Text(sesion.nombre, style = MaterialTheme.typography.headlineSmall)
                Text("Usuario: ${sesion.usuario}", style = MaterialTheme.typography.bodyMedium)
                Text("Rol: ${sesion.rol}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Text("Privilegios", style = MaterialTheme.typography.titleMedium)
                sesion.privilegios.sorted().forEach {
                    Text("• $it", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
            if (estado.pendientes > 0) {
                Text(
                    "Hay ${estado.pendientes} pesaje(s) sin enviar. Al cerrar sesión se borran de este equipo.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = { if (estado.pendientes > 0) confirmando = true else viewModel.salir() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cerrar sesión") }
        }
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("Cerrar sesión") },
            text = { Text("Hay ${estado.pendientes} pesaje(s) sin enviar que se perderán. ¿Desea continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = false
                    viewModel.salir()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { confirmando = false }) { Text("Cancelar") } }
        )
    }
}
