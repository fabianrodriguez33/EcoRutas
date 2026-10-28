package pe.upeu.ecorecicla

import android.app.Application
import org.koin.android.ext.koin.androidContext
import pe.upeu.ecorecicla.di.iniciarKoin

class EcoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        iniciarKoin(platformModule(this)) { androidContext(this@EcoApp) }
    }
}
