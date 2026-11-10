
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
import cl.duoc.huertaescolar.data.remote.aTareaLocal
import retrofit2.HttpException
import java.io.IOException

import android.net.Uri
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaType
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
            return
        }

        jobTareas = viewModelScope.launch {
            try {
                val token = "Bearer $accessToken"

                val tareasRemotas =
                    if (usuarioActivo.perfil == Perfil.ADMINISTRADOR) {
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

                val tareasConvertidas = tareasRemotas.map {
                    it.aTareaLocal()
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

    fun cargarEstudiantes() {

        viewModelScope.launch {

            val todosLosUsuarios = withContext(Dispatchers.IO) {
                repository.obtenerUsuarios()
            }

            _estudiantesDisponibles.value =
                todosLosUsuarios.filter { usuario ->
                    usuario.perfil == Perfil.ESTUDIANTE &&
                            usuario.estadoCuenta == "ACTIVA"
                }
        }
    }

    // =========================================================
    // CREAR TAREA
    // =========================================================

    fun crearTarea(onSuccess: () -> Unit) {

        errorDescripcionTarea = null
        errorTipoTarea = null
        errorFechaTarea = null
        errorResponsable = null

        var formularioValido = true

        if (descripcionTarea.isBlank()) {
            errorDescripcionTarea =
                "La descripción es obligatoria."
            formularioValido = false
        }

        if (tipoTarea.isBlank()) {
            errorTipoTarea =
                "Debe indicar el tipo (ej. Riego, Siembra)."
            formularioValido = false
        }

        if (fechaProgramada.isBlank()) {
            errorFechaTarea =
                "La fecha programada es obligatoria."
            formularioValido = false
        }

        if (responsableIdSeleccionado == null) {
            errorResponsable =
                "Debe asignar un responsable a la tarea."
            formularioValido = false
        }

        if (!formularioValido) return

        viewModelScope.launch {

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

            withContext(Dispatchers.IO) {
                repository.insertarTarea(nuevaTarea)
            }

            limpiarFormularioTarea()
            onSuccess()
        }
    }

    // =========================================================
    // ELIMINAR TAREA
    // =========================================================

    fun eliminarTarea(tarea: Tarea) {

        viewModelScope.launch(Dispatchers.IO) {
            repository.eliminarTarea(tarea)
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
                        archivo = multipart
                    )

                _tareasBase.value = _tareasBase.value.map { tarea ->
                    if (tarea.id == tareaId) {
                        tareaActualizada.aTareaLocal().copy(
                            comentarioEstudiante = comentario
                        )
                    } else {
                        tarea
                    }
                }

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
