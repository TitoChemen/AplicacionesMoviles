
package cl.duoc.huertaescolar.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

interface SectorApi {

    @GET("api/sectores")
    suspend fun listar(
        @Header("Authorization") token: String
    ): List<SectorResponseDto>

    @POST("api/sectores")
    suspend fun crear(
        @Header("Authorization") token: String,
        @Body sector: CrearSectorRequestDto
    ): SectorResponseDto

    @PUT("api/sectores/{id}")
    suspend fun actualizar(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body sector: CrearSectorRequestDto
    ): SectorResponseDto

    @DELETE("api/sectores/{id}")
    suspend fun eliminar(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    )
}
