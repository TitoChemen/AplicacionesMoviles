package cl.duoc.huertaescolar.data.remote

import retrofit2.http.GET
import retrofit2.http.Header

data class UsuarioCursoDto(
    val id: Long,
    val nombre: String,
    val correo: String,
    val rol: String,
    val activo: Boolean,
    val cursoId: Long?,
    val cursoCodigo: String?
)

interface UsuarioApi {
    @GET("api/usuarios")
    suspend fun listar(@Header("Authorization") token: String): List<UsuarioCursoDto>
}
