package cl.duoc.huertaescolar.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/** Clima exterior de referencia; no es una lectura del sensor de suelo. */
interface ClimaApi {
    @GET("v1/forecast")
    suspend fun actual(
        @Query("latitude") latitud: Double = -33.4489,
        @Query("longitude") longitud: Double = -70.6693,
        @Query("current") variables: String = "temperature_2m,relative_humidity_2m,precipitation",
        @Query("timezone") zonaHoraria: String = "America/Santiago"
    ): ClimaResponseDto
}

data class ClimaResponseDto(val current: ClimaActualDto?)

data class ClimaActualDto(
    val time: String?,
    @SerializedName("temperature_2m") val temperatura: Double?,
    @SerializedName("relative_humidity_2m") val humedadRelativa: Int?,
    val precipitation: Double?
)
