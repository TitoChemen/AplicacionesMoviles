
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

import android.util.Log
import cl.duoc.huertaescolar.data.remote.RetrofitClient
import cl.duoc.huertaescolar.data.remote.HistorialTareaDto
import cl.duoc.huertaescolar.data.remote.aTareaLocal
import retrofit2.HttpException
import java.io.IOException

import android.net.Uri
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlinx.coroutines.CancellationException

import cl.duoc.huertaescolar.data.remote.ValidacionRequestDto

class TareaViewModel(application: Application) : AndroidViewModel(application) {

    // =========================================================
    // REPOSITORIO Y BASE DE DATOS
    // =========================================================

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

    // =========================================================
    // FILTROS Y LISTADO DE TAREAS
    // =========================================================

    private val _filtroSeleccionado =
        MutableStateFlow(EstadoTarea.TODAS)

    val filtroSeleccionado: StateFlow<String> =
        _filtroSeleccionado.asStateFlow()

    // Lista original sin filtros
    private val _tareasBase =
        MutableStateFlow<List<Tarea>>(emptyList())

    // Lista pública para consultar una tarea por ID
    // independientemente del filtro seleccionado
    val tareasSinFiltrar: StateFlow<List<Tarea>> =
        _tareasBase.asStateFlow()

    // Las tareas del administrador global conservan su curso real para identificarlo.
    private val _intentosPorTarea = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val intentosPorTarea = _intentosPorTarea.asStateFlow()

    private val _historialTarea = MutableStateFlow<List<HistorialTareaDto>>(emptyList())
    val historialTarea = _historialTarea.asStateFlow()
    var errorHistorial by mutableStateOf<String?>(null)
        private set
    var cargandoHistorial by mutableStateOf(false)
        private set

    fun cargarHistorial(tareaId: Int, token: String?) {
        _historialTarea.value = emptyList()
        errorHistorial = null
        if (token.isNullOrBlank()) {
            errorHistorial = "Inicia sesión para ver el historial"
            return
        }
        viewModelScope.launch {
            cargandoHistorial = true
            try {
                _historialTarea.value = RetrofitClient.tareaApi.historial(
                    "Bearer $token", tareaId.toLong())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorHistorial = "No se pudo cargar el historial: ${e.message}"
            } finally {
                cargandoHistorial = false
            }
        }
    }

    private val _cursoPorTarea = MutableStateFlow<Map<Int, String>>(emptyMap())
    val cursoPorTarea: StateFlow<Map<Int, String>> = _cursoPorTarea.asStateFlow()

    // Lista utilizada por ListaTareasScreen
    val tareasFiltradas: StateFlow<List<Tarea>> =
        combine(
            _tareasBase,
            _filtroSeleccionado
        ) { lista, filtro ->

            when (filtro) {

                EstadoTarea.TODAS -> lista

                EstadoTarea.PENDIENTE_VALIDACION -> {
                    lista.filter { tarea ->
                        tarea.estado == EstadoTarea.PENDIENTE_VALIDACION ||
                                tarea.estado == "REALIZADA"
                    }
                }

                else -> {
                    lista.filter { tarea ->
                        tarea.estado == filtro
                    }
                }
            }

        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun actualizarFiltro(nuevoFiltro: String) {
        _filtroSeleccionado.value = nuevoFiltro
    }

    // =========================================================
    // CARGAR TAREAS SEGÚN EL ROL
    // =========================================================

    private var jobTareas: Job? = null


    fun cargarTareasSegunRol(
        usuarioActivo: Usuario?,
        accessToken: String?
    ) {
        jobTareas?.cancel()

        if (usuarioActivo == null || accessToken.isNullOrBlank()) {
            _tareasBase.value = emptyList()
            _cursoPorTarea.value = emptyMap()
            _intentosPorTarea.value = emptyMap()
            _historialTarea.value = emptyList()
            _estudiantesDisponibles.value = emptyList()
            _sectoresDisponibles.value = emptyList()
            _cursoIdPorEstudiante.value = emptyMap()
            limpiarFormularioTarea()
            return
        }

        jobTareas = viewModelScope.launch {
            try {
                val token = "Bearer $accessToken"

                val tareasRemotas =
                    if (Perfil.esPersonal(usuarioActivo.perfil)) {
                        RetrofitClient.tareaApi.listar(token)
                    } else {
                        val estudianteId = usuarioActivo.backendId

                        if (estudianteId == null) {
                            Log.e(
                                "TareaViewModel",
                                "El usuario no tiene backendId"
                            )
                            _tareasBase.value = emptyList()
                            return@launch
                        }

                        RetrofitClient.tareaApi.porEstudiante(
                            token = token,
                            estudianteId = estudianteId
                        )
                    }

                val tareasConvertidas = tareasRemotas.map { it.aTareaLocal() }
                _intentosPorTarea.value = tareasRemotas.associate { t ->
                    t.id.toInt() to t.intentoActual
                }

                // Obtiene códigos legibles 7-A/7-B para el administrador global.
                if (usuarioActivo.perfil == Perfil.ADMINISTRADOR) {
                    val usuarios = runCatching { RetrofitClient.usuarioApi.listar(token) }
                        .getOrElse { emptyList() }
                    val cursos = usuarios.mapNotNull { u ->
                        u.cursoId?.let { it to (u.cursoCodigo ?: "Curso #$it") }
                    }.toMap()
                    _cursoPorTarea.value = tareasRemotas.associate { t ->
                        t.id.toInt() to (t.cursoId?.let { cursos[it] ?: "Curso #$it" }
                            ?: "Sin curso")
                    }
                } else {
                    _cursoPorTarea.value = tareasRemotas.associate { t ->
                        t.id.toInt() to usuarioActivo.tallerAsignado
                    }
                }
                _tareasBase.value = tareasConvertidas

                Log.d(
                    "TareaViewModel",
                    "Tareas cargadas desde Spring Boot: ${tareasConvertidas.size}"
                )

            } catch (e: HttpException) {
                Log.e(
                    "TareaViewModel",
                    "Error HTTP ${e.code()} al obtener tareas",
                    e
                )
                _tareasBase.value = emptyList()

            } catch (e: IOException) {
                Log.e(
                    "TareaViewModel",
                    "Error de conexión con Tareas Service",
                    e
                )
                _tareasBase.value = emptyList()

            } catch (e: Exception) {
                Log.e(
                    "TareaViewModel",
                    "Error inesperado al cargar tareas",
                    e
                )
                _tareasBase.value = emptyList()
            }
        }
    }


    // =========================================================
    // FORMULARIO CREAR / EDITAR TAREA
    // =========================================================

    var descripcionTarea by mutableStateOf("")
    var tipoTarea by mutableStateOf("")
    var fechaProgramada by mutableStateOf("")

    var responsableIdSeleccionado by
    mutableStateOf<String?>(null)

    var errorDescripcionTarea by
    mutableStateOf<String?>(null)

    var errorTipoTarea by
    mutableStateOf<String?>(null)

    var errorFechaTarea by
    mutableStateOf<String?>(null)

    var errorResponsable by
    mutableStateOf<String?>(null)

    // =========================================================
    // LISTA DE ESTUDIANTES
    // =========================================================

    private val _estudiantesDisponibles =
        MutableStateFlow<List<Usuario>>(emptyList())

    val estudiantesDisponibles: StateFlow<List<Usuario>> =
        _estudiantesDisponibles.asStateFlow()

    private val _cursoIdPorEstudiante = MutableStateFlow<Map<Long, Long?>>(emptyMap())
    val cursoIdPorEstudiante: StateFlow<Map<Long, Long?>> = _cursoIdPorEstudiante.asStateFlow()

    // Estudiantes y sectores del MISMO curso, filtrados en el backend.
    private val _sectoresDisponibles =
        MutableStateFlow<List<cl.duoc.huertaescolar.data.remote.SectorResponseDto>>(emptyList())
    val sectoresDisponibles = _sectoresDisponibles.asStateFlow()
    var sectorIdSeleccionado by mutableStateOf<Long?>(null)
    var tareaIdEdicion by mutableStateOf<Int?>(null)
    var errorCreacion by mutableStateOf<String?>(null)

    fun cargarEstudiantes(accessToken: String?) {
        if (accessToken.isNullOrBlank()) {
            _estudiantesDisponibles.value = emptyList()
            _sectoresDisponibles.value = emptyList()
            _cursoIdPorEstudiante.value = emptyMap()
            return
        }
        viewModelScope.launch {
            try {
                val token = "Bearer $accessToken"
                val usuarios = RetrofitClient.usuarioApi.listar(token)
                    .filter { it.rol == Perfil.ESTUDIANTE && it.activo }
                _cursoIdPorEstudiante.value = usuarios.associate { it.id to it.cursoId }
                _estudiantesDisponibles.value = usuarios.map {
                        Usuario(
                            identificador = it.correo,
                            nombreFicticio = it.nombre,
                            perfil = it.rol,
                            tallerAsignado = it.cursoCodigo.orEmpty(),
                            estadoCuenta = "ACTIVA", contrasena = "",
                            backendId = it.id
                        )
                    }
                _sectoresDisponibles.value = RetrofitClient.sectorApi.listar(token)
                    .filter { it.estado == "ACTIVO" }
            } catch (e: Exception) {
                Log.e("TareaViewModel", "No se pudieron cargar usuarios/sectores", e)
                _estudiantesDisponibles.value = emptyList()
                _sectoresDisponibles.value = emptyList()
                _cursoIdPorEstudiante.value = emptyMap()
                errorCreacion = "No se pudieron consultar los datos del curso"
            }
        }
    }

    // =========================================================
    // CREAR/EDITAR TAREA EN SPRING BOOT, NO EN ROOM
    // =========================================================
    fun crearTarea(
        accessToken: String?,
        onSuccess: () -> Unit
    ) {
        errorCreacion = null
        errorDescripcionTarea = null
        errorTipoTarea = null
        errorFechaTarea = null
        errorResponsable = null

        if (accessToken.isNullOrBlank()) { errorCreacion = "Inicia sesión primero"; return }
        if (descripcionTarea.isBlank()) { errorDescripcionTarea = "Descripción obligatoria"; return }
        if (tipoTarea.isBlank()) { errorTipoTarea = "Título obligatorio"; return }
        if (fechaProgramada.isBlank()) { errorFechaTarea = "Fecha obligatoria"; return }
        val estudianteId = responsableIdSeleccionado?.toLongOrNull()
        if (estudianteId == null) { errorResponsable = "Selecciona un estudiante"; return }
        val sectorId = sectorIdSeleccionado
        if (sectorId == null) { errorCreacion = "Selecciona un sector"; return }

        viewModelScope.launch {
            try {
                val request = cl.duoc.huertaescolar.data.remote.TareaRequestDto(
                    sectorId = sectorId, titulo = tipoTarea.trim(),
                    descripcion = descripcionTarea.trim(), estudianteId = estudianteId,
                    fechaAsignacion = java.time.LocalDate.now().toString(),
                    fechaVencimiento = fechaProgramada.trim()
                )
                val token = "Bearer $accessToken"
                val resultado = if (tareaIdEdicion != null) {
                    RetrofitClient.tareaApi.actualizar(token, tareaIdEdicion!!.toLong(), request)
                } else {
                    RetrofitClient.tareaApi.crear(token, request)
                }
                _tareasBase.value = _tareasBase.value
                    .filterNot { it.id == resultado.id.toInt() } + resultado.aTareaLocal()
                _intentosPorTarea.value = _intentosPorTarea.value +
                    (resultado.id.toInt() to resultado.intentoActual)
                limpiarFormularioTarea()
                onSuccess()
            } catch (e: HttpException) {
                errorCreacion = if (e.code() == 403) "No puedes asignar tareas fuera de tu curso"
                    else "Error al guardar tarea: HTTP ${e.code()}"
            } catch (e: Exception) {
                errorCreacion = "No se pudo guardar la tarea: ${e.message}"
            }
        }
    }

    // =========================================================
    // ELIMINAR TAREA
    // =========================================================

    fun eliminarTarea(tarea: Tarea, accessToken: String?) {
        if (accessToken.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                RetrofitClient.tareaApi.eliminar("Bearer $accessToken", tarea.id.toLong())
                _tareasBase.value = _tareasBase.value.filterNot { it.id == tarea.id }
                _intentosPorTarea.value = _intentosPorTarea.value - tarea.id
            } catch (e: Exception) {
                Log.e("TareaViewModel", "No se pudo eliminar la tarea", e)
            }
        }
    }

    // =========================================================
    // LIMPIAR FORMULARIO
    // =========================================================

    fun limpiarFormularioTarea() {

        descripcionTarea = ""
        tipoTarea = ""
        fechaProgramada = ""
        responsableIdSeleccionado = null
        sectorIdSeleccionado = null
        tareaIdEdicion = null
        errorCreacion = null

        errorDescripcionTarea = null
        errorTipoTarea = null
        errorFechaTarea = null
        errorResponsable = null
    }

    // =========================================================
    // ACTUALIZAR TAREA
    // =========================================================

    fun actualizarTarea(
        tarea: Tarea,
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {

            withContext(Dispatchers.IO) {
                repository.actualizarTarea(tarea)
            }

            onSuccess()
        }
    }

    // =========================================================
    // PREPARAR EDICIÓN
    // =========================================================

    fun prepararEdicion(tarea: Tarea) {

        descripcionTarea = tarea.descripcion
        tipoTarea = tarea.tipo
        fechaProgramada = tarea.fechaProgramada
        responsableIdSeleccionado = tarea.asignadoA
        sectorIdSeleccionado = tarea.sectorId.toLong()
        tareaIdEdicion = tarea.id
    }

    // =========================================================
    // VALIDACIÓN DE EVIDENCIAS
    // =========================================================

    var comentarioValidacion by mutableStateOf("")


    fun evaluarTarea(
        tareaId: Int,
        aprobada: Boolean,
        accessToken: String?,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        if (accessToken.isNullOrBlank()) {
            Log.e("TareaViewModel", "No hay token para validar")
            onError()
            return
        }

        viewModelScope.launch {
            try {
                val tareaActual = _tareasBase.value
                    .find { it.id == tareaId }

                if (tareaActual == null) {
                    Log.e("TareaViewModel", "Tarea no encontrada: $tareaId")
                    onError()
                    return@launch
                }

                val puedeEvaluarse =
                    tareaActual.estado == EstadoTarea.PENDIENTE_VALIDACION ||
                            tareaActual.estado == "REALIZADA"

                if (!puedeEvaluarse) {
                    Log.e(
                        "TareaViewModel",
                        "La tarea no está pendiente de validación"
                    )
                    onError()
                    return@launch
                }

                val tareaActualizada =
                    RetrofitClient.tareaApi.validarEvidencia(
                        token = "Bearer $accessToken",
                        tareaId = tareaId.toLong(),
                        request = ValidacionRequestDto(
                            aprobada = aprobada,
                            observacion = comentarioValidacion.trim()
                        )
                    )

                _tareasBase.value = _tareasBase.value.map { tarea ->
                    if (tarea.id == tareaId) {
                        tareaActualizada.aTareaLocal()
                    } else {
                        tarea
                    }
                }

                comentarioValidacion = ""

                Log.d(
                    "TareaViewModel",
                    "Tarea $tareaId validada: ${tareaActualizada.estado}"
                )

                onSuccess()

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Log.e(
                    "TareaViewModel",
                    "Error al validar tarea $tareaId",
                    e
                )
                onError()
            }
        }
    }


    // =========================================================
    // REGISTRO DE EVIDENCIA FOTOGRÁFICA
    // =========================================================



    fun guardarEvidenciaPorId(
        tareaId: Int,
        comentario: String?,
        rutaFoto: String,
        accessToken: String?,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        if (accessToken.isNullOrBlank()) {
            Log.e("TareaViewModel", "No hay token de autenticación")
            onError()
            return
        }

        viewModelScope.launch {
            var archivoTemporal: File? = null

            try {
                val context = getApplication<Application>()
                val uri = Uri.parse(rutaFoto)

                val tipoMime = context.contentResolver
                    .getType(uri)
                    ?: "image/jpeg"

                val extension = MimeTypeMap
                    .getSingleton()
                    .getExtensionFromMimeType(tipoMime)
                    ?: "jpg"

                val archivo = withContext(Dispatchers.IO) {
                    File.createTempFile(
                        "evidencia_",
                        ".$extension",
                        context.cacheDir
                    ).also { destino ->
                        archivoTemporal = destino

                        context.contentResolver
                            .openInputStream(uri)
                            ?.use { entrada ->
                                destino.outputStream().use { salida ->
                                    entrada.copyTo(salida)
                                }
                            } ?: error("No se pudo abrir la fotografía")
                    }
                }

                val requestBody = archivo.asRequestBody(
                    tipoMime.toMediaType()
                )

                val multipart = MultipartBody.Part.createFormData(
                    "archivo",
                    archivo.name,
                    requestBody
                )

                val tareaActualizada =
                    RetrofitClient.tareaApi.subirEvidenciaArchivo(
                        token = "Bearer $accessToken",
                        tareaId = tareaId.toLong(),
                        archivo = multipart,
                        justificacion = comentario.orEmpty().trim()
                            .toRequestBody("text/plain".toMediaType())
                    )

                _tareasBase.value = _tareasBase.value.map { tarea ->
                    if (tarea.id == tareaId) {
                        tareaActualizada.aTareaLocal()
                    } else {
                        tarea
                    }
                }

                _intentosPorTarea.value = _intentosPorTarea.value +
                    (tareaId to tareaActualizada.intentoActual)

                Log.d(
                    "TareaViewModel",
                    "Evidencia enviada correctamente: tarea $tareaId"
                )

                onSuccess()

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Log.e(
                    "TareaViewModel",
                    "Error al subir evidencia de tarea $tareaId",
                    e
                )
                onError()

            } finally {
                archivoTemporal?.delete()
            }
        }
    }
}
