package pe.upeu.ecorecicla.domain.repository

/** Fuente de tiempo inyectable: permite fijar la hora en las pruebas y formatearla según la plataforma. */
interface Reloj {
    fun ahoraMs(): Long

    /** Fecha y hora local en ISO-8601 sin fracciones, por ejemplo 2026-10-28T09:15:30. */
    fun ahoraIso(): String

    /** Hora local legible (HH:mm) de una marca de tiempo. */
    fun formatearHora(ms: Long): String
}
