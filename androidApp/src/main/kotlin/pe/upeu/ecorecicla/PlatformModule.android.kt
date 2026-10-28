package pe.upeu.ecorecicla

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.Network
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import org.koin.dsl.module
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.remote.ApiConfig
import pe.upeu.ecorecicla.domain.repository.MonitorRed
import pe.upeu.ecorecicla.domain.repository.Reloj
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dependencias propias de Android: motor HTTP, controlador SQLite, almacén seguro, reloj y monitor de red. */
fun platformModule(contexto: Context) = module {
    single { ApiConfig(depuracion = (contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) }
    single<HttpClientEngine> { OkHttp.create() }
    single { crearControladorAndroid(contexto) }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(contexto) }
    single<Reloj> { RelojAndroid() }
    single<MonitorRed> { MonitorRedAndroid(contexto) }
}

class RelojAndroid : Reloj {
    override fun ahoraMs(): Long = System.currentTimeMillis()

    override fun ahoraIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

    override fun formatearHora(ms: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(ms))
}

/** Emite true cuando hay una red con acceso y false cuando se pierde (por ejemplo, modo avión). */
class MonitorRedAndroid(contexto: Context) : MonitorRed {
    private val administrador = contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val conectado: Flow<Boolean> = callbackFlow {
        val llamada = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(administrador.activeNetwork != null)
            }
        }
        trySend(administrador.activeNetwork != null)
        administrador.registerDefaultNetworkCallback(llamada)
        awaitClose { administrador.unregisterNetworkCallback(llamada) }
    }.conflate()
}
