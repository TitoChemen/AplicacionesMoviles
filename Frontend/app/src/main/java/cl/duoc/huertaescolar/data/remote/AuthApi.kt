package cl.duoc.huertaescolar.data.remote

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequestDto(
    val correo: String,
    val contrasena: String
)

data class AuthResponseDto(
    val accessToken: String,
    val tokenType: String,
    val usuario: JsonObject
)

interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): AuthResponseDto
}
