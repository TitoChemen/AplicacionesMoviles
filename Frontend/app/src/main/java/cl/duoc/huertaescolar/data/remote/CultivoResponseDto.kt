package cl.duoc.huertaescolar.data.remote

import cl.duoc.huertaescolar.data.local.entity.Cultivo

data class CultivoResponseDto(
    val id: Long,
    val sectorId: Long,
    val nombreComun: String,
    val descripcion: String?,
    val fechaSiembra: String?,
    val fechaCosechaEstimada: String?,
    val frecuenciaRiego: String?,
    val estadoDesarrollo: String?
)

data class CrearCultivoRequestDto(
    val sectorId: Long,
    val nombreComun: String,
    val descripcion: String,
    val fechaSiembra: String,
    val fechaCosechaEstimada: String,
    val frecuenciaRiego: String,
    val estadoDesarrollo: String
)

fun CultivoResponseDto.aCultivoLocal(): Cultivo {
    return Cultivo(
        id = id.toInt(),
        sectorId = sectorId.toInt(),
        nombreComun = nombreComun,
        descripcion = descripcion.orEmpty(),
        fechaSiembra = fechaSiembra.orEmpty(),
        fechaCosechaEstimada = fechaCosechaEstimada.orEmpty(),
        frecuenciaRiego = frecuenciaRiego.orEmpty(),
        estadoDesarrollo = estadoDesarrollo.orEmpty()
    )
}