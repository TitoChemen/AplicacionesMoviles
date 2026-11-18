package cl.duoc.huertaescolar.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class AlertaDto(
    val id: Long,
    val sectorId: Long,
    val tipo: String,
    val descripcion: String,
    val usuarioId: Long?,
    val cursoId: Long?,
    val fechaReporte: String?,
    val estado: String
)

data class CrearAlertaDto(val sectorId: Long, val tipo: String, val descripcion: String)
data class EstadoAlertaDto(val estado: String)

interface AlertaApi {
    @GET("api/alertas")
    suspend fun listar(@Header("Authorization") token: String): List<AlertaDto>

    @POST("api/alertas")
    suspend fun crear(@Header("Authorization") token: String,
                      @Body datos: CrearAlertaDto): AlertaDto

    @PATCH("api/alertas/{id}/estado")
    suspend fun cambiarEstado(@Header("Authorization") token: String,
                              @Path("id") id: Long,
                              @Body datos: EstadoAlertaDto): AlertaDto
}
