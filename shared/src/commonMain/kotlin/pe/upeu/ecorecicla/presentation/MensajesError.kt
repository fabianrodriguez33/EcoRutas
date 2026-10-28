package pe.upeu.ecorecicla.presentation

import pe.upeu.ecorecicla.domain.model.ErrorApi

/** Texto para el usuario de cada tipo de error: cada respuesta del servicio tiene una conducta distinta. */
fun ErrorApi.mensajeUsuario(): String = when (this) {
    is ErrorApi.Validacion -> campos.values.firstOrNull() ?: message.orEmpty()
    is ErrorApi.NoAutorizado -> "Su sesión venció. Inicie sesión nuevamente"
    is ErrorApi.SinPrivilegio -> "La operación no está autorizada para su usuario"
    is ErrorApi.NoEncontrado -> "El registro ya no existe. Se actualizó el listado"
    is ErrorApi.Conflicto -> message.orEmpty()
    is ErrorApi.Servidor -> "El servidor tuvo un problema. Intente de nuevo"
    is ErrorApi.SinConexion -> "Sin conexión con el servidor"
    is ErrorApi.TiempoAgotado -> "El servidor tardó demasiado en responder"
    is ErrorApi.Desconocido -> "Ocurrió un error inesperado"
}

/** Un 404 o un 403 no se arreglan reintentando; un fallo de servidor o de red sí. */
val ErrorApi.permiteReintento: Boolean
    get() = this is ErrorApi.Servidor || this is ErrorApi.SinConexion ||
        this is ErrorApi.TiempoAgotado || this is ErrorApi.Desconocido

fun Throwable.comoErrorApi(): ErrorApi = this as? ErrorApi ?: ErrorApi.Desconocido(this)
