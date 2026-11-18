package cl.duoc.huertaescolar.data.repository

import cl.duoc.huertaescolar.data.remote.ClimaApi

data class ClimaExterior(
    val temperaturaCelsius: Double,
    val humedadAirePorcentaje: Int,
    val precipitacionMm: Double,
    val hora: String
)

class ClimaRepository(private val api: ClimaApi) {
    suspend fun obtenerActual(): ClimaExterior {
        val actual = api.actual().current ?: error("La API no entregó clima actual")
        val temperatura = actual.temperatura ?: error("No hay temperatura exterior")
        val humedad = actual.humedadRelativa ?: error("No hay humedad del aire")
        val precipitacion = actual.precipitation ?: error("No hay datos de precipitación")
        require(temperatura.isFinite() && precipitacion.isFinite() && precipitacion >= 0) {
            "Datos meteorológicos inválidos"
        }
        require(humedad in 0..100) { "Humedad del aire fuera de rango" }
        return ClimaExterior(temperatura, humedad, precipitacion, actual.time.orEmpty())
    }
}
