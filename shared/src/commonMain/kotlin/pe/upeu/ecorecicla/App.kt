package pe.upeu.ecorecicla

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.upeu.ecorecicla.presentation.navigation.EcoNavHost
import pe.upeu.ecorecicla.presentation.theme.TemaEco

@Composable
fun App() {
    TemaEco {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            EcoNavHost()
        }
    }
}
