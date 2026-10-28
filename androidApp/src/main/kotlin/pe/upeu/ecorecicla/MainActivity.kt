package pe.upeu.ecorecicla

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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

        pedirAccesoRedLocal()

        setContent {
            App()
        }
    }

    /** Desde Android 17 conectarse a una dirección de red local (como 10.0.2.2) exige este permiso en ejecución. */
    private fun pedirAccesoRedLocal() {
        if (Build.VERSION.SDK_INT < 37) return
        if (checkSelfPermission(PERMISO_RED_LOCAL) == PackageManager.PERMISSION_GRANTED) return
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(PERMISO_RED_LOCAL)
    }

    private companion object {
        const val PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
