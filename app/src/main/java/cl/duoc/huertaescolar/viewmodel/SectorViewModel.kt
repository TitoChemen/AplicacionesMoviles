package cl.duoc.huertaescolar.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.huertaescolar.data.database.HuertaDatabase
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
        val database = HuertaDatabase.obtenerBaseDatos(application)
        // El orden coincide exactamente con HuertaRepository
        repository = HuertaRepository(
            sectorDao = database.sectorDao(),
            cultivoDao = database.cultivoDao(),
            tareaDao = database.tareaDao(),
            usuarioDao = database.usuarioDao(),
            alertaDao = database.alertaDao()
        )
    }

    // Flujos observables llamados desde los métodos del repositorio
    val sectores: StateFlow<List<Sector>> = repository.obtenerSectores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cultivos: StateFlow<List<Cultivo>> = repository.obtenerCultivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Formulario de Sectores
    var nombreSector by mutableStateOf("")
    var descripcionSector by mutableStateOf("")
    var cultivoSeleccionadoId by mutableStateOf<Int?>(null)

    // Datos de sensores
    var temperaturaSimulada by mutableStateOf(23.5)
    var humedadSimulada by mutableStateOf(68.0)

    fun registrarSector(onSuccess: () -> Unit) {
        if (nombreSector.isNotBlank()) {
            viewModelScope.launch {
                val nuevoSector = Sector(
                    nombre = nombreSector,
                    descripcion = descripcionSector,
                    codigo = "SEC-${System.currentTimeMillis().toString().takeLast(4)}",
                    ubicacion = "Zona Principal",
                    estado = "ACTIVO",
                    fechaCreacion = "2026-03-01",
                    responsableId = "admin1"
                )
                repository.insertarSector(nuevoSector)
                nombreSector = ""
                descripcionSector = ""
                onSuccess()
            }
        }
    }

    fun registrarCultivo(nombre: String, descripcion: String) {
        if (nombre.isNotBlank()) {
            viewModelScope.launch {
                val nuevoCultivo = Cultivo(
                    sectorId = 1,
                    nombreComun = nombre,
                    descripcion = descripcion,
                    fechaSiembra = "2026-03-01",
                    fechaCosechaEstimada = "2026-06-01",
                    frecuenciaRiego = "Diario",
                    estadoDesarrollo = "CRECIMIENTO"
                )
                repository.insertarCultivo(nuevoCultivo)
            }
        }
    }
}