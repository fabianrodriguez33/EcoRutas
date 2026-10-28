package pe.upeu.ecorecicla

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.domain.model.Sesion

/**
 * Guarda el token y los datos de la sesión en preferencias cifradas (AES-256) cuya clave maestra vive
 * en el Android Keystore. Sin cifrar, cualquier lectura del archivo de preferencias expondría el token.
 */
@Suppress("DEPRECATION")
class AlmacenSeguroAndroid(contexto: Context) : AlmacenSeguro {

    private val preferencias: SharedPreferences = run {
        val clave = MasterKey.Builder(contexto).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            contexto,
            "ecorutas_sesion_segura",
            clave,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun guardar(sesion: Sesion) {
        preferencias.edit()
            .putString(TOKEN, sesion.token)
            .putString(USUARIO, sesion.usuario)
            .putString(NOMBRE, sesion.nombre)
            .putString(ROL, sesion.rol)
            .putStringSet(PRIVILEGIOS, sesion.privilegios.toSet())
            .apply()
    }

    override fun leer(): Sesion? {
        val token = preferencias.getString(TOKEN, null) ?: return null
        val usuario = preferencias.getString(USUARIO, null) ?: return null
        return Sesion(
            token = token,
            usuario = usuario,
            rol = preferencias.getString(ROL, "").orEmpty(),
            privilegios = preferencias.getStringSet(PRIVILEGIOS, emptySet()).orEmpty().toSet(),
            nombre = preferencias.getString(NOMBRE, usuario).orEmpty()
        )
    }

    override fun borrar() {
        preferencias.edit().remove(TOKEN).remove(USUARIO).remove(NOMBRE).remove(ROL).remove(PRIVILEGIOS).apply()
    }

    override fun propietarioDatos(): String? = preferencias.getString(PROPIETARIO, null)

    override fun fijarPropietarioDatos(usuario: String?) {
        preferencias.edit().apply {
            if (usuario == null) remove(PROPIETARIO) else putString(PROPIETARIO, usuario)
        }.apply()
    }

    private companion object {
        const val TOKEN = "token"
        const val USUARIO = "usuario"
        const val NOMBRE = "nombre"
        const val ROL = "rol"
        const val PRIVILEGIOS = "privilegios"
        const val PROPIETARIO = "propietario_datos"
    }
}
