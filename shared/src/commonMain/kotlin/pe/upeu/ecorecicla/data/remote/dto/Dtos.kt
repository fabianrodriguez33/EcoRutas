package pe.upeu.ecorecicla.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.Sesion

@Serializable
data class LoginPeticionDto(val usuario: String, val contrasena: String)

@Serializable
data class LoginRespuestaDto(
    val token: String,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: List<String> = emptyList()
)

@Serializable
data class UsuarioActualDto(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: List<String> = emptyList()
)

@Serializable
data class PuntoRutaDto(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int,
    val atendido: Boolean,
    val motivoOmision: String? = null
)

@Serializable
data class RutaDto(
    val id: Long,
    val fecha: String,
    val estado: String,
    val puntos: List<PuntoRutaDto> = emptyList()
) {
    val cerrada: Boolean get() = estado == "CERRADA"
}

@Serializable
data class PesajeDto(
    val id: Long,
    val puntoId: Long,
    val rutaId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,
    val operario: String,
    @SerialName("anulado") val estaAnulado: Boolean = false,
    val motivoAnulacion: String? = null,
    val observacion: String? = null,
    val humedadEstimada: Double? = null
)

/** Envoltorio de paginación común a todos los listados del servicio. */
@Serializable
data class PaginaDto<T>(
    @SerialName("contenido") val items: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)

@Serializable
data class PesajeCrearDto(
    val rutaId: Long,
    val puntoId: Long,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val observacion: String? = null,
    val humedadEstimada: Double? = null
)

@Serializable
data class PesajeEditarDto(
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val observacion: String? = null,
    val humedadEstimada: Double? = null
)

/** Formato de error común del servicio. */
@Serializable
data class ErrorDto(
    val timestamp: String? = null,
    val status: Int = 0,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)

fun LoginRespuestaDto.aDominio(): Sesion =
    Sesion(token = token, usuario = usuario, rol = rol, privilegios = privilegios.toSet(), nombre = nombre)

fun PuntoRutaDto.aDominio(rutaId: Long): PuntoAcopio = PuntoAcopio(
    id = id, razonSocial = razonSocial, direccion = direccion, zona = zona, materialHabitual = materialHabitual,
    secuencia = secuencia, atendido = atendido, rutaId = rutaId
)

fun PesajeDto.aDominio(): Pesaje = Pesaje(
    id = id, puntoId = puntoId, fechaHora = fechaHora, material = material, pesoBruto = pesoBruto, tara = tara,
    pesoNeto = pesoNeto, operario = operario,
    estado = if (estaAnulado) EstadoPesaje.Anulado(motivoAnulacion ?: "Sin motivo") else EstadoPesaje.Registrado,
    pendiente = false, rutaId = rutaId, observacion = observacion, humedadEstimada = humedadEstimada
)
