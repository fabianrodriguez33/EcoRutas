package pe.upeu.ecorecicla.domain.model

data class RutaInfo(
    val id: Long,
    val fecha: String,
    val cerrada: Boolean,
    val ultimaSincronizacion: Long?   // milisegundos desde 1970; nulo si nunca se sincronizó
)

data class ResumenSync(val enviados: Int, val rechazados: Int, val recibidos: Int)

data class ResultadoEnvio(val enviados: Int, val rechazados: Int)

data class ResultadoRegistro(val pesaje: Pesaje, val enviado: Boolean)
