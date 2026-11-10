package cl.duoc.huertaescolar.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // No registrar cuerpos: el login contiene contraseñas
                    // y las respuestas contienen tokens JWT.
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val tareaApi: TareaApi by lazy {
        retrofit.create(TareaApi::class.java)
    }

    val sectorApi: SectorApi by lazy {
        retrofit.create(SectorApi::class.java)
    }

    val cultivoApi: CultivoApi by lazy {
        retrofit.create(CultivoApi::class.java)
    }


}
