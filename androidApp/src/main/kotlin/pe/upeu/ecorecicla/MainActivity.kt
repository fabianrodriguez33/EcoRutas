package pe.upeu.ecorecicla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import pe.upeu.ecorecicla.presentation.navigation.Navegador

class MainActivity : ComponentActivity() {

    private val navegador: Navegador by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // El botón atrás retrocede en la pila propia de la aplicación; en la raíz cierra la actividad.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navegador.atras()) finish()
            }
        })

        setContent {
            App()
        }
    }
}
