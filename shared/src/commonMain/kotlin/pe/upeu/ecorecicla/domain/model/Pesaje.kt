package pe.upeu.ecorecicla.domain.model

data class Pesaje(
    val id: Long,                // negativo mientras esté pendiente de envío
    val puntoId: Long,
    val fechaHora: String,       // ISO-8601
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,        // lo calcula el servicio
    val operario: String,
    val estado: EstadoPesaje,
    val pendiente: Boolean,
    val rutaId: Long = 0,
    val observacion: String? = null,
    val errorEnvio: String? = null,
    val humedadEstimada: Double? = null   // porcentaje de 0 a 100; opcional
)
