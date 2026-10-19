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

    // Campos del formulario
    var nombreSector by mutableStateOf("")
    var ubicacionSector by mutableStateOf("") // Añadido porque el checklist pide "ubicación"
    var descripcionSector by mutableStateOf("")

    // Estados para los mensajes de error bajo los campos
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

        // 3. Guardar solo si la información es válida
        if (formularioValido) {
            viewModelScope.launch {
                val nuevoSector = Sector(
                    nombre = nombreSector,
                    descripcion = descripcionSector,
                    codigo = "SEC-${System.currentTimeMillis().toString().takeLast(4)}",
                    ubicacion = ubicacionSector, // Usamos la variable real
                    estado = "ACTIVO",
                    fechaCreacion = System.currentTimeMillis().toString(), // Fecha dinámica
                    responsableId = responsableId // Identificador real del admin que lo crea
                )
                repository.insertarSector(nuevoSector)

                // Limpiar el formulario tras el éxito
                nombreSector = ""
                descripcionSector = ""
                ubicacionSector = ""

                onSuccess()
            }
        }
    }

    // Funciones para actualizar el estado desde los Composables
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

    // Estados de error
    var errorNombreCultivo by mutableStateOf<String?>(null)
    var errorFechasCultivo by mutableStateOf<String?>(null)

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
            // Checklist: La cosecha estimada no puede ser anterior a la siembra.
            // Si usamos formato YYYY-MM-DD, podemos compararlos directamente como Strings
            if (fechaCosecha < fechaSiembra) {
                errorFechasCultivo = "Incoherencia temporal: la cosecha no puede ser antes de la siembra."
                formularioValido = false
            }
        }

        // 3. Guardar si todo es válido
        if (formularioValido) {
            viewModelScope.launch {
                val nuevoCultivo = Cultivo(
                    sectorId = sectorId,
                    nombreComun = nombreCultivo,
                    descripcion = descripcionCultivo,
                    fechaSiembra = fechaSiembra,
                    fechaCosechaEstimada = fechaCosecha,
                    frecuenciaRiego = "Diario", // Puedes agregar un campo para esto luego
                    estadoDesarrollo = "CRECIMIENTO"
                )
                repository.insertarCultivo(nuevoCultivo)

                // Limpiar formulario
                nombreCultivo = ""
                descripcionCultivo = ""
                fechaSiembra = ""
                fechaCosecha = ""

                onSuccess()
            }
        }
    }
}

//ESTE ES EL ANTERIOR, SEGUN FUENTES ESTÁ MEJOR EL OTRO (SEGUN LA IA xd)
/*package cl.duoc.huertaescolar.viewmodel

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
    var ubicacionSector by mutableStateOf("")
    var descripcionSector by mutableStateOf("")
    var cultivoSeleccionadoId by mutableStateOf<Int?>(null)

    // Estados para los mensajes de error bajo los campos
    var errorNombreSector by mutableStateOf<String?>(null)
    var errorUbicacionSector by mutableStateOf<String?>(null)

    // Datos de sensores
    var temperaturaSimulada by mutableStateOf(23.5)
    var humedadSimulada by mutableStateOf(68.0)

    fun registrarSector(onSuccess: () -> Unit) {
        errorNombreSector = null
        errorUbicacionSector = null

        var formularioValido = true
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
}*/