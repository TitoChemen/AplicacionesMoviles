package cl.duoc.huertaescolar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.huertaescolar.data.repository.ClimaExterior
import cl.duoc.huertaescolar.data.repository.ClimaRepository
import cl.duoc.huertaescolar.data.remote.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoClima(
    val cargando: Boolean = false,
    val dato: ClimaExterior? = null,
    val error: String? = null
)

class ClimaViewModel : ViewModel() {
    private val repository = ClimaRepository(RetrofitClient.climaApi)
    private val _estado = MutableStateFlow(EstadoClima())
    val estado = _estado.asStateFlow()

    fun actualizar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, error = null)
            try {
                _estado.value = EstadoClima(dato = repository.obtenerActual())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(cargando = false,
                    error = "Clima no disponible. Puedes reintentar.")
            }
        }
    }
}
