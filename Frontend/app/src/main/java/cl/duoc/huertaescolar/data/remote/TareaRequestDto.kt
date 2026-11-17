package cl.duoc.huertaescolar.data.remote

data class TareaRequestDto(
    val sectorId: Long,
    val titulo: String,
    val descripcion: String,
    val estudianteId: Long,
    val fechaAsignacion: String,
    val fechaVencimiento: String
)
