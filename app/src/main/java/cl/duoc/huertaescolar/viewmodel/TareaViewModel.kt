package cl.duoc.huertaescolar.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.huertaescolar.data.EstadoTarea
import cl.duoc.huertaescolar.data.Perfil
import cl.duoc.huertaescolar.data.database.HuertaDatabase
import cl.duoc.huertaescolar.data.local.entity.Tarea
import cl.duoc.huertaescolar.data.local.entity.Usuario
import cl.duoc.huertaescolar.data.repository.HuertaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TareaViewModel(application: Application) : AndroidViewModel(application) {

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

    // 1. Filtro seleccionado (por defecto, todas)
    private val _filtroSeleccionado = MutableStateFlow(EstadoTarea.TODAS)
    val filtroSeleccionado: StateFlow<String> = _filtroSeleccionado.asStateFlow()

    // 2. Lista base (sin filtrar) según el rol del usuario activo
    private val _tareasBase = MutableStateFlow<List<Tarea>>(emptyList())
    val todasLasTareas: StateFlow<List<Tarea>> = _tareasBase.asStateFlow()

    // 3. Lista filtrada: la lista de tareas observa este flujo
    val tareasFiltradas: StateFlow<List<Tarea>> =
        combine(_tareasBase, _filtroSeleccionado) { lista, filtro ->
            if (filtro == EstadoTarea.TODAS) lista else lista.filter { it.estado == filtro }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun actualizarFiltro(nuevoFiltro: String) {
        _filtroSeleccionado.value = nuevoFiltro
    }


    // 4. Decide qué datos traer según el rol (administrador: todas; estudiante: las suyas)
    private var jobTareas: Job? = null

    fun cargarTareasSegunRol(usuarioActivo: Usuario?) {
        jobTareas?.cancel()

        if (usuarioActivo == null) {
            _tareasBase.value = emptyList()
            return
        }

        val flujo = if (usuarioActivo.perfil == Perfil.ADMINISTRADOR) {
            repository.obtenerTodasLasTareasFlow()
        } else {
            repository.obtenerTareasDelEstudiante(usuarioActivo.identificador)
        }

        jobTareas = viewModelScope.launch {
            flujo.collect { lista -> _tareasBase.value = lista }
        }
    }

    // --- Campos del formulario Crear Tarea ---
    var descripcionTarea by mutableStateOf("")
    var tipoTarea by mutableStateOf("") // Ej: Riego, Siembra, Mantenimiento
    var fechaProgramada by mutableStateOf("")
    var responsableIdSeleccionado by mutableStateOf<String?>(null)

    // Estados de error
    var errorDescripcionTarea by mutableStateOf<String?>(null)
    var errorTipoTarea by mutableStateOf<String?>(null)
    var errorFechaTarea by mutableStateOf<String?>(null)
    var errorResponsable by mutableStateOf<String?>(null)

    // Lista de estudiantes para el menú desplegable
    private val _estudiantesDisponibles = MutableStateFlow<List<Usuario>>(emptyList())
    val estudiantesDisponibles: StateFlow<List<Usuario>> = _estudiantesDisponibles.asStateFlow()

    fun cargarEstudiantes() {
        viewModelScope.launch {
            val todosLosUsuarios = repository.obtenerUsuarios()
            _estudiantesDisponibles.value = todosLosUsuarios.filter {
                it.perfil == Perfil.ESTUDIANTE && it.estadoCuenta == "ACTIVA"
            }
        }
    }

    fun crearTarea(onSuccess: () -> Unit) {
        errorDescripcionTarea = null
        errorTipoTarea = null
        errorFechaTarea = null
        errorResponsable = null
        var formularioValido = true

        if (descripcionTarea.isBlank()) { errorDescripcionTarea = "La descripción es obligatoria."; formularioValido = false }
        if (tipoTarea.isBlank()) { errorTipoTarea = "Debe indicar el tipo (ej. Riego, Siembra)."; formularioValido = false }
        if (fechaProgramada.isBlank()) { errorFechaTarea = "La fecha programada es obligatoria."; formularioValido = false }
        if (responsableIdSeleccionado == null) { errorResponsable = "Debe asignar un responsable a la tarea."; formularioValido = false }

        if (formularioValido) {
            viewModelScope.launch(Dispatchers.IO) {
                val nuevaTarea = Tarea(
                    tipo = tipoTarea,
                    descripcion = descripcionTarea,
                    sectorId = 1, // Por defecto el sector 1 (precargado)
                    cultivoId = null,
                    fechaProgramada = fechaProgramada,
                    asignadoA = responsableIdSeleccionado ?: "",
                    estado = EstadoTarea.PENDIENTE,
                    comentarioEstudiante = null,
                    comentarioDocente = null,
                    rutaFotografia = null
                )
                repository.insertarTarea(nuevaTarea)

                descripcionTarea = ""
                tipoTarea = ""
                fechaProgramada = ""
                responsableIdSeleccionado = null

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    // --- NUEVO: Función para ELIMINAR tareas (CRUD Admin) ---
    fun eliminarTarea(tarea: Tarea) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.eliminarTarea(tarea)
        }
    }

    // para limpiar los estados de formulario

    fun limpiarFormularioTarea() {
        descripcionTarea = ""
        tipoTarea = ""
        fechaProgramada = ""
        responsableIdSeleccionado = null
        errorDescripcionTarea = null
        errorTipoTarea = null
        errorFechaTarea = null
        errorResponsable = null
    }

    // --- NUEVO: Función para EDITAR tareas existentes ---
    fun actualizarTarea(tarea: Tarea, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.actualizarTarea(tarea)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    // Precargar datos en los campos cuando el admin quiere editar una tarea
    fun prepararEdicion(tarea: Tarea) {
        descripcionTarea = tarea.descripcion
        tipoTarea = tarea.tipo
        fechaProgramada = tarea.fechaProgramada
        responsableIdSeleccionado = tarea.asignadoA
    }

    // --- Campos para la Entrega del Estudiante ---
    var comentarioEntrega by mutableStateOf("")
    var rutaFotografiaEntrega by mutableStateOf<String?>(null)

    fun entregarTarea(tareaId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val tareaActual = _tareasBase.value.find { it.id == tareaId }

            if (tareaActual != null) {
                // Si la tarea fue rechazada previamente, creamos un NUEVO registro de envío
                // para conservar el historial de rechazos y feedback anterior en la BD.
                if (tareaActual.estado == EstadoTarea.RECHAZADA) {
                    val nuevaEntrega = Tarea(
                        tipo = tareaActual.tipo,
                        descripcion = tareaActual.descripcion,
                        sectorId = tareaActual.sectorId,
                        cultivoId = tareaActual.cultivoId,
                        fechaProgramada = tareaActual.fechaProgramada,
                        asignadoA = tareaActual.asignadoA,
                        estado = EstadoTarea.PENDIENTE_VALIDACION,
                        comentarioEstudiante = comentarioEntrega,
                        comentarioDocente = null, // Limpiamos para la nueva revisión
                        rutaFotografia = rutaFotografiaEntrega
                    )
                    repository.insertarTarea(nuevaEntrega)
                } else {
                    // Si es una tarea pendiente normal, la actualizamos de forma común
                    val tareaActualizada = tareaActual.copy(
                        estado = EstadoTarea.PENDIENTE_VALIDACION,
                        comentarioEstudiante = comentarioEntrega,
                        rutaFotografia = rutaFotografiaEntrega
                    )
                    repository.actualizarTarea(tareaActualizada)
                }

                comentarioEntrega = ""
                rutaFotografiaEntrega = null

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    // --- Campos para la Validación del Docente ---
    var comentarioValidacion by mutableStateOf("")

    fun evaluarTarea(tareaId: Int, aprobada: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val tareaActual = _tareasBase.value.find { it.id == tareaId }

            if (tareaActual != null) {
                val nuevoEstado = if (aprobada) EstadoTarea.VALIDADA else EstadoTarea.RECHAZADA

                val tareaActualizada = tareaActual.copy(
                    estado = nuevoEstado,
                    comentarioDocente = comentarioValidacion.ifBlank { null }
                )
                repository.actualizarTarea(tareaActualizada)

                comentarioValidacion = ""
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }
}