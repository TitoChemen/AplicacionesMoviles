package cl.duoc.huertaescolar.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.huertaescolar.data.database.HuertaDataBase
import cl.duoc.huertaescolar.data.local.entity.Cultivo
import cl.duoc.huertaescolar.data.local.entity.Sector
import cl.duoc.huertaescolar.data.repository.HuertaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SectorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HuertaRepository

    init {
        val database = HuertaDataBase.getDatabase(application)
        repository = HuertaRepository(
            sectorDao = database.sectorDao(),
            cultivoDao = database.cultivoDao(),
            tareaDao = database.tareaDao(),
            usuarioDao = database.usuarioDao()
        )
    }

    // Listas observables desde la base de datos
    val sectores: StateFlow<List<Sector>> = repository.obtenerSectores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cultivos: StateFlow<List<Cultivo>> = repository.obtenerCultivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estados para el formulario de Sectores
    var nombreSector by mutableStateOf("")
    var descripcionSector by mutableStateOf("")
    var cultivoSeleccionadoId by mutableStateOf<Long?>(null)

    // Datos simulados de sensores para mostrar en la interfaz
    var temperaturaSimulada by mutableStateOf(23.5)
    var humedadSimulada by mutableStateOf(68.0)

    fun registrarSector(onSuccess: () -> Unit) {
        if (nombreSector.isNotBlank()) {
            viewModelScope.launch {
                val nuevoSector = Sector(
                    nombre = nombreSector,
                    descripcion = descripcionSector
                )
                repository.insertarSector(nuevoSector)
                // Limpiar campos
                nombreSector = ""
                descripcionSector = ""
                onSuccess()
            }
        }
    }

    fun registrarCultivo(nombre: String, tipo: String) {
        if (nombre.isNotBlank()) {
            viewModelScope.launch {
                val nuevoCultivo = Cultivo(
                    nombre = nombre,
                    tipo = tipo
                )
                repository.insertarCultivo(nuevoCultivo)
            }
        }
    }
}