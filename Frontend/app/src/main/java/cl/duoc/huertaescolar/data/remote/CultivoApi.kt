package cl.duoc.huertaescolar.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

interface CultivoApi {

    @GET("api/cultivos")
    suspend fun listar(
        @Header("Authorization") token: String
    ): List<CultivoResponseDto>

    @GET("api/cultivos/sector/{sectorId}")
    suspend fun porSector(
        @Header("Authorization") token: String,
        @Path("sectorId") sectorId: Long
    ): List<CultivoResponseDto>

    @POST("api/cultivos")
    suspend fun crear(
        @Header("Authorization") token: String,
        @Body cultivo: CrearCultivoRequestDto
    ): CultivoResponseDto

    @PUT("api/cultivos/{id}")
    suspend fun actualizar(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body cultivo: CrearCultivoRequestDto
    ): CultivoResponseDto

    @DELETE("api/cultivos/{id}")
    suspend fun eliminar(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    )
}