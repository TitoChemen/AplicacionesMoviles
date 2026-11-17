package cl.duoc.huertaescolar.data.remote

import retrofit2.http.GET
import retrofit2.http.Header

data class CursoDto(val id: Long, val codigo: String, val nombre: String)

interface CursoApi {
    @GET("api/cursos")
    suspend fun listar(@Header("Authorization") token: String): List<CursoDto>
}
