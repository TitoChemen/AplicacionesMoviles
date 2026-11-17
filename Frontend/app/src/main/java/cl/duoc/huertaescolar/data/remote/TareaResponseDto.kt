
package cl.duoc.huertaescolar.data.remote

import cl.duoc.huertaescolar.data.local.entity.Tarea

data class TareaResponseDto(
    val id: Long,
    val titulo: String,
    val descripcion: String?,
    val estado: String,
    val estudianteId: Long?,
    val sectorId: Long,
    val fechaAsignacion: String?,
    val fechaVencimiento: String?,
    val evidenciaUrl: String?,
    val observacionDocente: String?,
    val comentarioEstudiante: String? = null,
    val cursoId: Long? = null,
    val intentoActual: Int = 0
)

fun TareaResponseDto.aTareaLocal(): Tarea {
    return Tarea(
        id = id.toInt(),
        tipo = titulo,
        descripcion = descripcion.orEmpty(),
        sectorId = sectorId.toInt(),
        cultivoId = null,
        fechaProgramada = fechaVencimiento
            ?: fechaAsignacion
            ?: "Sin fecha",
        asignadoA = estudianteId?.toString().orEmpty(),
        estado = estado,
        comentarioEstudiante = comentarioEstudiante,
        comentarioDocente = observacionDocente,
        rutaFotografia = evidenciaUrl
    )
}
