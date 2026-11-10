
package cl.duoc.huertaescolar.data.remote

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming



interface TareaApi {

    // Obtener todas las tareas
    @GET("api/tareas")
    suspend fun listar(
        @Header("Authorization") token: String
    ): List<TareaResponseDto>

    // Obtener tareas de un estudiante
    @GET("api/tareas/estudiante/{id}")
    suspend fun porEstudiante(
        @Header("Authorization") token: String,
        @Path("id") estudianteId: Long
    ): List<TareaResponseDto>

    // Subir evidencia fotográfica al backend
    @Multipart
    @POST("api/tareas/{id}/evidencias/archivo")
    suspend fun subirEvidenciaArchivo(
        @Header("Authorization") token: String,
        @Path("id") tareaId: Long,
        @Part archivo: MultipartBody.Part
    ): TareaResponseDto


    @PATCH("api/tareas/{id}/validacion")
    suspend fun validarEvidencia(
        @Header("Authorization") token: String,
        @Path("id") tareaId: Long,
        @Body request: ValidacionRequestDto
    ): TareaResponseDto

    @Streaming
    @GET("api/tareas/{id}/evidencias/archivo")
    suspend fun descargarEvidencia(
        @Header("Authorization") token: String,
        @Path("id") tareaId: Long,
        @Query("nombre") nombre: String
    ): Response<ResponseBody>


}
