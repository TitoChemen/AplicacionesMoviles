package cl.duoc.huertaescolar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.huertaescolar.data.remote.AlertaDto
import cl.duoc.huertaescolar.data.remote.CrearAlertaDto
import cl.duoc.huertaescolar.data.remote.EstadoAlertaDto
import cl.duoc.huertaescolar.data.remote.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertaViewModel : ViewModel() {
    private val _alertas = MutableStateFlow<List<AlertaDto>>(emptyList())
    val alertas = _alertas.asStateFlow()
    private var tokenSesion: String? = null
    private var cargaJob: Job? = null
    var cargando = androidx.compose.runtime.mutableStateOf(false)
        private set
    var error = androidx.compose.runtime.mutableStateOf<String?>(null)
        private set

    fun configurarSesion(token: String?) {
        if (tokenSesion == token) return
        cargaJob?.cancel()
        tokenSesion = token
        _alertas.value = emptyList()
        cargando.value = false
        error.value = null
    }

    fun cargar() {
        val sesion = tokenSesion?.takeIf { it.isNotBlank() } ?: return
        cargaJob?.cancel()
        cargaJob = viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val datos = RetrofitClient.alertaApi.listar("Bearer $sesion")
                if (sesion == tokenSesion) _alertas.value = datos
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sesion == tokenSesion) error.value = "No se pudieron cargar los reportes."
            } finally {
                if (sesion == tokenSesion) cargando.value = false
            }
        }
    }

    fun reportar(sectorId: Long?, tipo: String, descripcion: String, alGuardar: () -> Unit) {
        val sesion = tokenSesion ?: return
        if (sectorId == null || tipo.isBlank() || descripcion.isBlank()) {
            error.value = "Elige sector, tipo y escribe una descripción."
            return
        }
        if (cargando.value) return
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                RetrofitClient.alertaApi.crear("Bearer $sesion",
                    CrearAlertaDto(sectorId, tipo, descripcion.trim()))
                if (sesion == tokenSesion) {
                    cargar()
                    alGuardar()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sesion == tokenSesion) error.value = "No se pudo enviar el reporte: ${e.message}"
            } finally {
                if (sesion == tokenSesion) cargando.value = false
            }
        }
    }

    fun cambiarEstado(id: Long, estado: String) {
        val sesion = tokenSesion ?: return
        if (cargando.value || estado !in setOf("EN_REVISION", "RESUELTA")) return
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val actualizado = RetrofitClient.alertaApi.cambiarEstado(
                    "Bearer $sesion", id, EstadoAlertaDto(estado))
                if (sesion == tokenSesion)
                    _alertas.value = _alertas.value.map { if (it.id == id) actualizado else it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sesion == tokenSesion) error.value = "No se pudo actualizar el reporte: ${e.message}"
            } finally {
                if (sesion == tokenSesion) cargando.value = false
            }
        }
    }
}
