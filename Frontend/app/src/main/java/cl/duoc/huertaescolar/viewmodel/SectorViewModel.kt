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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SectorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HuertaRepository

    init {
        val database = HuertaDatabase.obtenerBaseDatos(application)
        repository = HuertaRepository(
            sectorDao = database.sectorDao(),
            cultivoDao = database.cultivoDao(),
            tareaDao = database.tareaDao(),
            usuarioDao = database.usuarioDao(),
            alertaDao = database.alertaDao()
        )
    }

    val sectores: StateFlow<List<Sector>> = repository.obtenerSectores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cultivos: StateFlow<List<Cultivo>> = repository.obtenerCultivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Campos del formulario de Sector
    var nombreSector by mutableStateOf("")
    var ubicacionSector by mutableStateOf("")
    var descripcionSector by mutableStateOf("")

    // Estados para los mensajes de error bajo los campos de Sector
    var errorNombreSector by mutableStateOf<String?>(null)
    var errorUbicacionSector by mutableStateOf<String?>(null)

    // Datos de sensores simulados
    var temperaturaSimulada by mutableStateOf(23.5)
    var humedadSimulada by mutableStateOf(68.0)

    fun registrarSector(responsableId: String, onSuccess: () -> Unit) {
        // 1. Limpiar errores previos
        errorNombreSector = null
        errorUbicacionSector = null

        var formularioValido = true

        // 2. Validaciones de campos obligatorios
        if (nombreSector.isBlank()) {
            errorNombreSector = "El nombre del sector es obligatorio."
            formularioValido = false
        }

        if (ubicacionSector.isBlank()) {
            errorUbicacionSector = "Debes indicar la ubicación del sector."
            formularioValido = false
        }

        // 3. Guardar en segundo plano solo si la información es válida
        if (formularioValido) {
            viewModelScope.launch(Dispatchers.IO) {
                val nuevoSector = Sector(
                    nombre = nombreSector,
                    descripcion = descripcionSector,
                    codigo = "SEC-${System.currentTimeMillis().toString().takeLast(4)}",
                    ubicacion = ubicacionSector,
                    estado = "ACTIVO",
                    fechaCreacion = System.currentTimeMillis().toString(),
                    responsableId = responsableId
                )
                repository.insertarSector(nuevoSector)

                // Volvemos al hilo principal para limpiar estados y disparar la navegación/éxito
                withContext(Dispatchers.Main) {
                    nombreSector = ""
                    descripcionSector = ""
                    ubicacionSector = ""
                    onSuccess()
                }
            }
        }
    }

    // Funciones para actualizar el estado del Sector desde los Composables
    fun onNombreSectorChange(nuevoNombre: String) {
        nombreSector = nuevoNombre
        if (errorNombreSector != null) errorNombreSector = null
    }

    fun onUbicacionSectorChange(nuevaUbicacion: String) {
        ubicacionSector = nuevaUbicacion
        if (errorUbicacionSector != null) errorUbicacionSector = null
    }

    // --- Campos del formulario de Cultivo ---
    var nombreCultivo by mutableStateOf("")
    var descripcionCultivo by mutableStateOf("")
    var fechaSiembra by mutableStateOf("") // Formato esperado: YYYY-MM-DD
    var fechaCosecha by mutableStateOf("")

    // Estados de error de Cultivo
    var errorNombreCultivo by mutableStateOf<String?>(null)
    var errorFechasCultivo by mutableStateOf<String?>(null)

    fun eliminarSector(sector: Sector) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.eliminarSector(sector) // O si tu repositorio usa el ID, usa repository.eliminarSectorPorId(sector.id)
        }
    }

    fun registrarCultivo(sectorId: Int, onSuccess: () -> Unit) {
        // 1. Limpiar errores
        errorNombreCultivo = null
        errorFechasCultivo = null
        var formularioValido = true

        // 2. Validar campos obligatorios
        if (nombreCultivo.isBlank()) {
            errorNombreCultivo = "El nombre del cultivo es obligatorio."
            formularioValido = false
        }

        if (fechaSiembra.isBlank() || fechaCosecha.isBlank()) {
            errorFechasCultivo = "Debes ingresar ambas fechas (YYYY-MM-DD)."
            formularioValido = false
        } else {
            if (fechaCosecha < fechaSiembra) {
                errorFechasCultivo = "Incoherencia temporal: la cosecha no puede ser antes de la siembra."
                formularioValido = false
            }
        }

        // 3. Guardar en segundo plano si todo es válido
        if (formularioValido) {
            viewModelScope.launch(Dispatchers.IO) {
                val nuevoCultivo = Cultivo(
                    sectorId = sectorId,
                    nombreComun = nombreCultivo,
                    descripcion = descripcionCultivo,
                    fechaSiembra = fechaSiembra,
                    fechaCosechaEstimada = fechaCosecha,
                    frecuenciaRiego = "Diario",
                    estadoDesarrollo = "CRECIMIENTO"
                )
                repository.insertarCultivo(nuevoCultivo)

                // Volvemos al hilo principal para limpiar y avisar el éxito
                withContext(Dispatchers.Main) {
                    nombreCultivo = ""
                    descripcionCultivo = ""
                    fechaSiembra = ""
                    fechaCosecha = ""
                    onSuccess()
                }
            }
        }
    }
}