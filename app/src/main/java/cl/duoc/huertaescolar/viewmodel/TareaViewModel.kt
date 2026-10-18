package cl.duoc.huertaescolar.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.huertaescolar.data.database.HuertaDatabase
import cl.duoc.huertaescolar.data.local.entity.Tarea
import cl.duoc.huertaescolar.data.local.entity.Usuario
import cl.duoc.huertaescolar.data.repository.HuertaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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

    // 1. Estado del filtro seleccionado (Por defecto mostramos todas)
    private val _filtroSeleccionado = MutableStateFlow("Todas")
    val filtroSeleccionado: StateFlow<String> = _filtroSeleccionado.asStateFlow()

    // 2. Flujo crudo desde la base de datos (asumiendo que repository.obtenerTareas() existe)
    // 2. Flujo base vacío (se llenará cuando sepamos quién es el usuario)
    private val _tareasBase = MutableStateFlow<List<Tarea>>(emptyList())

    // 3. Flujo filtrado: La vista SOLAMENTE observa este flujo
    val tareasFiltradas: StateFlow<List<Tarea>> = combine(_tareasBase, _filtroSeleccionado) { lista, filtro ->
        if (filtro == "Todas") {
            lista
        } else {
            lista.filter { it.estado == filtro }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun actualizarFiltro(nuevoFiltro: String) {
        _filtroSeleccionado.value = nuevoFiltro
    }

    // 4. NUEVA FUNCIÓN: Decide qué datos traer según el rol
    fun cargarTareasSegunRol(usuarioActivo: Usuario?) {
        if (usuarioActivo == null) return

        fun cargarTareasSegunRol(usuarioActivo: Usuario?) {
            if (usuarioActivo == null) return

            viewModelScope.launch {
                if (usuarioActivo.perfil == "Administrador") {
                    // Asignación directa porque este devuelve List<Tarea>
                    val lista = repository.obtenerTodasLasTareas()
                    _tareasBase.value = lista
                } else {
                    // Como el estudiante devuelve un Flow, usamos .collect
                    repository.obtenerHistorialTareas(estudianteId = usuarioActivo.identificador.toString()).collect { listaEstudiante ->
                        _tareasBase.value = listaEstudiante
                    }
                }
            }
        }
    }
    // --- Campos del Formulario Crear Tarea ---
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
            // Obtenemos la lista directamente
            val todosLosUsuarios = repository.obtenerUsuarios()

            // Filtramos usando "perfil" en lugar de "rol"
            _estudiantesDisponibles.value = todosLosUsuarios.filter { it.perfil == "Colaborador" }
        }
    }

    fun crearTarea(onSuccess: () -> Unit) {
        // 1. Limpiar errores
        errorDescripcionTarea = null
        errorTipoTarea = null
        errorFechaTarea = null
        errorResponsable = null
        var formularioValido = true

        // 2. Checklist: Validar campos obligatorios
        if (descripcionTarea.isBlank()) { errorDescripcionTarea = "La descripción es obligatoria."; formularioValido = false }
        if (tipoTarea.isBlank()) { errorTipoTarea = "Debe indicar el tipo (ej. Riego, Siembra)."; formularioValido = false }
        if (fechaProgramada.isBlank()) { errorFechaTarea = "La fecha programada es obligatoria."; formularioValido = false }
        if (responsableIdSeleccionado == null) { errorResponsable = "Debe asignar un responsable a la tarea."; formularioValido = false }

        // 3. Guardar en Base de Datos
        if (formularioValido) {
            viewModelScope.launch {
                val nuevaTarea = Tarea(
                    tipo = tipoTarea,
                    descripcion = descripcionTarea,
                    sectorId = 1, // Le pasamos un 1 por defecto por ahora (o el ID del sector que quieras)
                    cultivoId = null, // Puede ser null según tu clase
                    fechaProgramada = fechaProgramada,
                    asignadoA = responsableIdSeleccionado ?: "", // Este es el identificador del estudiante
                    estado = "PENDIENTE", // Lo pusiste en mayúsculas en tu modelo
                    comentarioEstudiante = null,
                    comentarioDocente = null,
                    rutaFotografia = null
                )
                repository.insertarTarea(nuevaTarea)

                // Limpiar formulario tras éxito
                descripcionTarea = ""
                tipoTarea = ""
                fechaProgramada = ""
                responsableIdSeleccionado = null

                onSuccess()
            }
        }
    }
    // --- Campos para la Entrega del Estudiante ---
    var comentarioEntrega by mutableStateOf("")
    var rutaFotografiaEntrega by mutableStateOf<String?>(null)

    fun entregarTarea(tareaId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Buscamos la tarea exacta en nuestra lista actual cargada en memoria
            val tareaActual = _tareasBase.value.find { it.id == tareaId }

            if (tareaActual != null) {
                // Creamos una copia actualizada con los datos del estudiante
                val tareaActualizada = tareaActual.copy(
                    estado = "PENDIENTE_VALIDACION", // Checklist: Flujo de estados
                    comentarioEstudiante = comentarioEntrega,
                    rutaFotografia = rutaFotografiaEntrega
                )

                // IMPORTANTE: Asegúrate de tener actualizarTarea() en tu DAO y Repositorio
                repository.actualizarTarea(tareaActualizada)

                // Limpiamos los campos después de entregar
                comentarioEntrega = ""
                rutaFotografiaEntrega = null

                onSuccess()
            }
        }
    }
    // --- Campos para la Validación del Docente ---
    var comentarioValidacion by mutableStateOf("")

    fun evaluarTarea(tareaId: Int, aprobada: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val tareaActual = _tareasBase.value.find { it.id == tareaId }

            if (tareaActual != null) {
                // Si la aprueba, el estado cambia a Validada. Si no, a Rechazada.
                val nuevoEstado = if (aprobada) "Validada" else "Rechazada"

                val tareaActualizada = tareaActual.copy(
                    estado = nuevoEstado,
                    comentarioDocente = comentarioValidacion.ifBlank { null }
                )

                repository.actualizarTarea(tareaActualizada)

                // Limpiamos el campo
                comentarioValidacion = ""
                onSuccess()
            }
        }
    }
}