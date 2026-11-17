package cl.duoc.huertaescolar.data.remote

data class HistorialTareaDto(
    val id: Long,
    val tareaId: Long,
    val intento: Int,
    val accion: String,
    val actorId: Long,
    val comentario: String?,
    val evidenciaUrl: String?,
    val fecha: String
)
