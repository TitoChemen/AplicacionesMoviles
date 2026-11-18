package cl.duoc.huertaescolar

import cl.duoc.huertaescolar.data.remote.ClimaActualDto
import cl.duoc.huertaescolar.data.remote.ClimaApi
import cl.duoc.huertaescolar.data.remote.ClimaResponseDto
import cl.duoc.huertaescolar.data.repository.ClimaRepository
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ClimaRepositoryTest {
    private fun repo(respuesta: ClimaResponseDto) = ClimaRepository(object : ClimaApi {
        override suspend fun actual(latitud: Double, longitud: Double,
                                    variables: String, zonaHoraria: String) = respuesta
    })

    @Test fun transformaRespuestaRealDeOpenMeteo() = runBlocking {
        val respuesta = Gson().fromJson("""
            {"current":{"time":"2026-11-17T15:00","temperature_2m":23.5,
            "relative_humidity_2m":61,"precipitation":0.2}}
        """.trimIndent(), ClimaResponseDto::class.java)
        val lectura = repo(respuesta).obtenerActual()
        assertEquals(23.5, lectura.temperaturaCelsius, 0.001)
        assertEquals(61, lectura.humedadAirePorcentaje)
        assertEquals(0.2, lectura.precipitacionMm, 0.001)
        assertEquals("2026-11-17T15:00", lectura.hora)
    }

    @Test fun rechazaRespuestaIncompleta() = runBlocking {
        try {
            repo(ClimaResponseDto(null)).obtenerActual()
            fail("Debió rechazar una respuesta sin clima actual")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("clima actual"))
        }
    }

    @Test fun rechazaHumedadFueraDeRango() = runBlocking {
        try {
            repo(ClimaResponseDto(ClimaActualDto("15:00", 23.5, 120, 0.0))).obtenerActual()
            fail("Debió rechazar humedad inválida")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Humedad"))
        }
    }

    @Test fun rechazaPrecipitacionNegativa() = runBlocking {
        try {
            repo(ClimaResponseDto(ClimaActualDto("15:00", 23.5, 61, -1.0))).obtenerActual()
            fail("Debió rechazar precipitación inválida")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("meteorológicos"))
        }
    }
}
