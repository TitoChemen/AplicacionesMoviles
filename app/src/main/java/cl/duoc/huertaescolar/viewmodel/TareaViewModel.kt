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

    // 1. Filtro seleccionado (por defecto, TODAS)
    private val _filtroSeleccionado = MutableStateFlow(EstadoTarea.TODAS)
    val filtroSeleccionado: StateFlow<String> = _filtroSeleccionado.asStateFlow()

    // 2. Lista base (sin filtrar) según el rol del usuario activo
    private val _tareasBase = MutableStateFlow<List<Tarea>>(emptyList())

    // 3. Lista filtrada: la lista de tareas en la UI observa este flujo
    val tareasFiltradas: StateFlow<List<Tarea>> =
        combine(_tareasBase, _filtroSeleccionado) { lista, filtro ->
            if (filtro == EstadoTarea.TODAS) lista else lista.filter { it.estado == filtro }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun actualizarFiltro(nuevoFiltro: String) {
        _filtroSeleccionado.value = nuevoFiltro
    }

    // 4. Carga tareas segun rol (Admin: todas; Estudiante: sus tareas)
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

    // --- Campos del formulario Crear / Editar Tarea ---
    var descripcionTarea by mutableStateOf("")
    var tipoTarea by mutableStateOf("")
    var fechaProgramada by mutableStateOf("")
    var responsableIdSeleccionado by mutableStateOf<String?>(null)

    // Estados de error para validaciones
    var errorDescripcionTarea by mutableStateOf<String?>(null)
    var errorTipoTarea by mutableStateOf<String?>(null)
    var errorFechaTarea by mutableStateOf<String?>(null)
    var errorResponsable by mutableStateOf<String?>(null)

    // Lista de estudiantes para el desplegable de asignación
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
                    sectorId = 1,
                    cultivoId = null,
                    fechaProgramada = fechaProgramada,
                    asignadoA = responsableIdSeleccionado ?: "",
                    estado = EstadoTarea.PENDIENTE,
                    comentarioEstudiante = null,
                    comentarioDocente = null,
                    rutaFotografia = null
                )
                repository.insertarTarea(nuevaTarea)
                limpiarFormularioTarea()

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    fun eliminarTarea(tarea: Tarea) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.eliminarTarea(tarea)
        }
    }

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

    fun actualizarTarea(tarea: Tarea, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.actualizarTarea(tarea)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun prepararEdicion(tarea: Tarea) {
        descripcionTarea = tarea.descripcion
        tipoTarea = tarea.tipo
        fechaProgramada = tarea.fechaProgramada
        responsableIdSeleccionado = tarea.asignadoA
    }

    // --- Validación / Evaluación por parte del Docente ---
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
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    // --- Registro de Evidencia Fotográfica (Invocado desde EvidenciaScreen) ---
    fun guardarEvidenciaPorId(tareaId: Int, comentario: String?, rutaFoto: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tareas = repository.obtenerTodasLasTareas()
            val tareaEncontrada = tareas.find { it.id == tareaId }
            tareaEncontrada?.let { tarea ->
                repository.marcarTareaComoRealizada(tarea, comentario, rutaFoto)
            }
        }
    }
}