package cl.duoc.huertaescolar.data.remote

import cl.duoc.huertaescolar.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val BASE_URL = BuildConfig.BACKEND_BASE_URL

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

    // Cliente separado: los fallos de Open-Meteo no afectan los microservicios propios.
    private val climaRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val climaApi: ClimaApi by lazy { climaRetrofit.create(ClimaApi::class.java) }

    val alertaApi: AlertaApi by lazy { retrofit.create(AlertaApi::class.java) }

    val usuarioApi: UsuarioApi by lazy { retrofit.create(UsuarioApi::class.java) }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val tareaApi: TareaApi by lazy {
        retrofit.create(TareaApi::class.java)
    }

    val cursoApi: CursoApi by lazy { retrofit.create(CursoApi::class.java) }

    val sectorApi: SectorApi by lazy {
        retrofit.create(SectorApi::class.java)
    }

    val cultivoApi: CultivoApi by lazy {
        retrofit.create(CultivoApi::class.java)
    }


}
