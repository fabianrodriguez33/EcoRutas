package pe.upeu.ecorecicla.data.local

import pe.upeu.ecorecicla.domain.model.Sesion

/**
 * Almacenamiento seguro de la sesión. En Android lo respalda EncryptedSharedPreferences sobre el
 * almacén de claves (Keystore); el código común solo conoce este contrato.
 */
interface AlmacenSeguro {
    fun guardar(sesion: Sesion)

    fun leer(): Sesion?

    /** Borra el token y los datos de la sesión. */
    fun borrar()

    /** Usuario dueño de los datos locales; evita que el turno siguiente herede datos de otro usuario. */
    fun propietarioDatos(): String?

    fun fijarPropietarioDatos(usuario: String?)
}
