package pe.upeu.ecorecicla.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.ResultadoRegistro
import pe.upeu.ecorecicla.domain.model.capturarErrores
import pe.upeu.ecorecicla.domain.repository.PesajeRepository

class ObservarPesajesUseCase(private val repositorio: PesajeRepository) {
    operator fun invoke(puntoId: Long): Flow<List<Pesaje>> = repositorio.observarPesajes(puntoId)
}

class ObservarPesajeUseCase(private val repositorio: PesajeRepository) {
    operator fun invoke(id: Long): Flow<Pesaje?> = repositorio.observarPesaje(id)
}

class ObservarPendientesUseCase(private val repositorio: PesajeRepository) {
    operator fun invoke(): Flow<Int> = repositorio.observarPendientes()
}

/** Datos del formulario ya validados; la validación es la misma con y sin conexión. */
data class DatosPesaje(
    val material: String, val pesoBruto: Double, val tara: Double, val observacion: String?,
    val humedadEstimada: Double?
)

object ValidadorPesaje {
    fun validar(material: String, bruto: String, tara: String, observacion: String, humedad: String = ""): DatosPesaje {
        val errores = LinkedHashMap<String, String>()
        val pesoBruto = bruto.aNumero()
        val pesoTara = if (tara.isBlank()) 0.0 else tara.aNumero()
        if (material.isBlank()) errores["material"] = "El material es obligatorio"
        else if (material.trim().length > 60) errores["material"] = "Máximo 60 caracteres"
        if (pesoBruto == null) errores["pesoBruto"] = "Ingrese un peso bruto válido"
        else if (pesoBruto <= 0.0) errores["pesoBruto"] = "El peso bruto debe ser mayor que cero"
        if (pesoTara == null) errores["tara"] = "Ingrese una tara válida"
        else if (pesoTara < 0.0) errores["tara"] = "La tara no puede ser negativa"
        val pesoHumedad = if (humedad.isBlank()) null else humedad.aNumero()
        if (humedad.isNotBlank() && (pesoHumedad == null || pesoHumedad < 0.0 || pesoHumedad > 100.0)) {
            errores["humedadEstimada"] = "La humedad debe estar entre 0 y 100"
        }
        if (observacion.trim().length > 200) errores["observacion"] = "Máximo 200 caracteres"
        if (errores.isEmpty() && pesoBruto!! - pesoTara!! <= 0.0) {
            errores["pesoBruto"] = "El peso bruto debe ser mayor que la tara"
        }
        if (errores.isNotEmpty()) throw ErrorApi.Validacion(errores)
        return DatosPesaje(material.trim(), pesoBruto!!, pesoTara!!, observacion.trim().ifEmpty { null }, pesoHumedad)
    }

    private fun String.aNumero(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}

class RegistrarPesajeUseCase(private val repositorio: PesajeRepository) {
    suspend operator fun invoke(
        puntoId: Long, rutaId: Long, material: String, bruto: String, tara: String, observacion: String,
        humedad: String = ""
    ): Result<ResultadoRegistro> = capturarErrores {
        val datos = ValidadorPesaje.validar(material, bruto, tara, observacion, humedad)
        repositorio.registrar(
            puntoId, rutaId, datos.material, datos.pesoBruto, datos.tara, datos.observacion, datos.humedadEstimada
        )
    }
}

class CorregirPesajeUseCase(private val repositorio: PesajeRepository) {
    suspend operator fun invoke(
        id: Long, material: String, bruto: String, tara: String, observacion: String, humedad: String = ""
    ): Result<ResultadoRegistro> = capturarErrores {
        val datos = ValidadorPesaje.validar(material, bruto, tara, observacion, humedad)
        repositorio.corregir(
            id, datos.material, datos.pesoBruto, datos.tara, datos.observacion, datos.humedadEstimada
        )
    }
}

class AnularPesajeUseCase(private val repositorio: PesajeRepository) {
    suspend operator fun invoke(id: Long, motivo: String): Result<ResultadoRegistro> = capturarErrores {
        if (motivo.isBlank()) throw ErrorApi.Validacion(mapOf("motivo" to "Indique el motivo de la anulación"))
        repositorio.anular(id, motivo.trim())
    }
}
