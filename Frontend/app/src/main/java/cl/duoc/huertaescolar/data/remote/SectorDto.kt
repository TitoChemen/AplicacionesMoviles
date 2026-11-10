
package cl.duoc.huertaescolar.data.remote

import cl.duoc.huertaescolar.data.local.entity.Sector

data class SectorResponseDto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val descripcion: String?,
    val ubicacion: String?,
    val estado: String,
    val fechaCreacion: String?,
    val responsableId: String?
)

data class CrearSectorRequestDto(
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val ubicacion: String,
    val estado: String,
    val fechaCreacion: String,
    val responsableId: String
)

fun SectorResponseDto.aSectorLocal(): Sector = Sector(
    id = id.toInt(),
    codigo = codigo,
    nombre = nombre,
    descripcion = descripcion.orEmpty(),
    ubicacion = ubicacion.orEmpty(),
    estado = estado,
    fechaCreacion = fechaCreacion.orEmpty(),
    responsableId = responsableId.orEmpty()
)
