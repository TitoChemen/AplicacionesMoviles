
package cl.duoc.huertaescolar.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.huertaescolar.data.local.entity.Cultivo
import cl.duoc.huertaescolar.data.local.entity.Sector
import cl.duoc.huertaescolar.data.remote.CrearCultivoRequestDto
import cl.duoc.huertaescolar.data.remote.CursoDto
import cl.duoc.huertaescolar.data.remote.CrearSectorRequestDto
import cl.duoc.huertaescolar.data.remote.RetrofitClient
import cl.duoc.huertaescolar.data.remote.aCultivoLocal
import cl.duoc.huertaescolar.data.remote.aSectorLocal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import retrofit2.HttpException
import org.json.JSONObject

class SectorViewModel(application: Application) :
    AndroidViewModel(application) {

    private val _sectores =
        MutableStateFlow<List<Sector>>(emptyList())
    val sectores = _sectores.asStateFlow()

    private val _cultivos =
        MutableStateFlow<List<Cultivo>>(emptyList())
    val cultivos = _cultivos.asStateFlow()

    private var accessToken: String? = null
    private val _cursosDisponibles = MutableStateFlow<List<CursoDto>>(emptyList())
    val cursosDisponibles = _cursosDisponibles.asStateFlow()
    var cursoSeleccionadoId by mutableStateOf<Long?>(null)
    private val _idCursoPorSector = MutableStateFlow<Map<Int, Long?>>(emptyMap())
    private val _cursoPorSector = MutableStateFlow<Map<Int, String>>(emptyMap())
    val cursoPorSector = _cursoPorSector.asStateFlow()

    private fun actualizarNombreCursoSector() {
        val nombrePorId = _cursosDisponibles.value.associate { it.id to it.codigo }
        _cursoPorSector.value = _idCursoPorSector.value.mapValues { (_, cursoId) ->
            cursoId?.let { nombrePorId[it] ?: "Curso #$it" } ?: "Sin curso"
        }
    }

    fun cargarCursos() {
        if (accessToken.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                val cursos = RetrofitClient.cursoApi.listar(tokenAutenticado())
                _cursosDisponibles.value = cursos
                actualizarNombreCursoSector()
                if (cursoSeleccionadoId != null && cursos.none { it.id == cursoSeleccionadoId })
                    cursoSeleccionadoId = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorGeneral = "No se pudo obtener la lista de cursos: ${e.message}"
            }
        }
    }


    var cargando by mutableStateOf(false)
        private set

    var errorGeneral by mutableStateOf<String?>(null)
        private set

    // =====================================
    // SESIÓN
    // =====================================

    fun configurarSesion(token: String?) {
        if (accessToken == token) return

        accessToken = token
        _sectores.value = emptyList()
        _cultivos.value = emptyList()
        _cursosDisponibles.value = emptyList()
        _cursoPorSector.value = emptyMap()
        _idCursoPorSector.value = emptyMap()
        cursoSeleccionadoId = null
        errorGeneral = null

        if (!token.isNullOrBlank()) {
            cargarDatos()
        }
    }

    private fun tokenAutenticado(): String {
        val token = accessToken
            ?.takeIf { it.isNotBlank() }
            ?: error("No hay sesión activa")

        return if (token.startsWith("Bearer ")) {
            token
        } else {
            "Bearer $token"
        }
    }

    // =====================================
    // CARGAR SECTORES Y CULTIVOS
    // =====================================

    fun cargarDatos() {
        if (accessToken.isNullOrBlank()) return

        viewModelScope.launch {
            cargando = true
            errorGeneral = null

            try {
                val token = tokenAutenticado()

                val sectoresRemotos =
                    RetrofitClient.sectorApi.listar(token)

                val cultivosRemotos =
                    RetrofitClient.cultivoApi.listar(token)

                _sectores.value =
                    sectoresRemotos.map { it.aSectorLocal() }
                _idCursoPorSector.value = sectoresRemotos.associate { s ->
                    s.id.toInt() to s.cursoId
                }
                actualizarNombreCursoSector()

                _cultivos.value =
                    cultivosRemotos.map { it.aCultivoLocal() }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    "SectorViewModel",
                    "Error cargando sectores y cultivos",
                    e
                )

                errorGeneral =
                    "No se pudieron cargar los datos: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    // =====================================
    // FORMULARIO SECTOR
    // =====================================

    var nombreSector by mutableStateOf("")
    var ubicacionSector by mutableStateOf("")
    var descripcionSector by mutableStateOf("")

    var errorNombreSector by mutableStateOf<String?>(null)
    var errorUbicacionSector by mutableStateOf<String?>(null)

    var temperaturaSimulada by mutableStateOf(23.5)
    var humedadSimulada by mutableStateOf(68.0)

    fun onNombreSectorChange(valor: String) {
        nombreSector = valor
        errorNombreSector = null
    }

    fun onUbicacionSectorChange(valor: String) {
        ubicacionSector = valor
        errorUbicacionSector = null
    }

    // =====================================
    // REGISTRAR SECTOR EN MARIADB
    // =====================================

    fun registrarSector(
        responsableId: String,
        esAdminGlobal: Boolean = false,
        onSuccess: () -> Unit
    ) {
        if (cargando) return

        errorNombreSector = null
        errorUbicacionSector = null
        errorGeneral = null

        if (nombreSector.isBlank()) {
            errorNombreSector =
                "El nombre del sector es obligatorio."
        }

        if (ubicacionSector.isBlank()) {
            errorUbicacionSector =
                "La ubicación del sector es obligatoria."
        }

        if (esAdminGlobal && cursoSeleccionadoId == null) {
            errorGeneral = "Selecciona un curso para el sector."
            return
        }
        if (
            errorNombreSector != null ||
            errorUbicacionSector != null
        ) {
            return
        }

        viewModelScope.launch {
            cargando = true

            try {
                val request = CrearSectorRequestDto(
                    codigo = "SEC-" +
                            UUID.randomUUID()
                                .toString()
                                .take(8)
                                .uppercase(),
                    nombre = nombreSector.trim(),
                    descripcion = descripcionSector.trim(),
                    ubicacion = ubicacionSector.trim(),
                    estado = "ACTIVO",
                    fechaCreacion = LocalDate.now().toString(),
                    responsableId = responsableId,
                    cursoId = if (esAdminGlobal) cursoSeleccionadoId else null
                )

                val creado =
                    RetrofitClient.sectorApi.crear(
                        token = tokenAutenticado(),
                        sector = request
                    )

                _sectores.value =
                    _sectores.value
                        .filterNot { it.id == creado.id.toInt() } +
                            creado.aSectorLocal()

                nombreSector = ""
                ubicacionSector = ""
                descripcionSector = ""
                cursoSeleccionadoId = null

                onSuccess()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    "SectorViewModel",
                    "Error registrando sector",
                    e
                )

                errorGeneral =
                    "No se pudo registrar el sector: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    // =====================================
    // EDITAR / ARCHIVAR / ELIMINAR SECTORES
    // =====================================

    private fun mensajeErrorSector(e: Exception, accion: String): String {
        if (e is HttpException) {
            val detalle = runCatching {
                val cuerpo = e.response()?.errorBody()?.string().orEmpty()
                val json = JSONObject(cuerpo)
                json.optString("error").takeIf { it.isNotBlank() }
                    ?: json.optString("detail").takeIf { it.isNotBlank() }
                    ?: json.optString("message").takeIf { it.isNotBlank() }
            }.getOrNull()
            return when (e.code()) {
                400 -> detalle ?: "Los datos del sector no son válidos."
                401 -> "La sesión venció. Vuelve a iniciar sesión."
                403 -> "Tu cuenta no tiene permisos para $accion este sector."
                404 -> "El sector ya no existe en la base de datos."
                409 -> detalle ?: "El sector tiene datos relacionados y no se puede eliminar. Puedes archivarlo."
                else -> detalle ?: "Error del servidor (${e.code()}) al $accion el sector."
            }
        }
        return "No se pudo $accion el sector: ${e.localizedMessage ?: "Error de conexión"}"
    }

    fun limpiarErrorSector() { errorGeneral = null }

    fun editarSector(
        sector: Sector,
        nombre: String,
        ubicacion: String,
        descripcion: String,
        estado: String,
        onSuccess: () -> Unit
    ) {
        if (cargando) return
        errorGeneral = null
        if (nombre.isBlank() || ubicacion.isBlank()) {
            errorGeneral = "El nombre y la ubicación son obligatorios."
            return
        }
        if (estado !in setOf("ACTIVO", "INACTIVO")) {
            errorGeneral = "Selecciona un estado válido."
            return
        }

        viewModelScope.launch {
            cargando = true
            try {
                val actualizado = RetrofitClient.sectorApi.actualizar(
                    tokenAutenticado(),
                    sector.id.toLong(),
                    CrearSectorRequestDto(
                        codigo = sector.codigo, // Código inmutable: protege referencias existentes.
                        nombre = nombre.trim(),
                        descripcion = descripcion.trim(),
                        ubicacion = ubicacion.trim(),
                        estado = estado,
                        fechaCreacion = sector.fechaCreacion,
                        responsableId = sector.responsableId,
                        cursoId = _idCursoPorSector.value[sector.id]
                    )
                )
                _sectores.value = _sectores.value.map {
                    if (it.id == sector.id) actualizado.aSectorLocal() else it
                }
                _idCursoPorSector.value = _idCursoPorSector.value +
                    (sector.id to actualizado.cursoId)
                actualizarNombreCursoSector()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SectorViewModel", "Error editando sector", e)
                errorGeneral = mensajeErrorSector(e, "editar")
            } finally {
                cargando = false
            }
        }
    }

    fun archivarSector(sector: Sector, onSuccess: () -> Unit) {
        editarSector(sector, sector.nombre, sector.ubicacion,
            sector.descripcion, "INACTIVO", onSuccess)
    }

    fun eliminarSector(sector: Sector, onSuccess: () -> Unit) {
        if (cargando) return
        errorGeneral = null
        viewModelScope.launch {
            cargando = true
            try {
                RetrofitClient.sectorApi.eliminar(
                    token = tokenAutenticado(), id = sector.id.toLong()
                )
                _sectores.value = _sectores.value.filterNot { it.id == sector.id }
                _cultivos.value = _cultivos.value.filterNot { it.sectorId == sector.id }
                _idCursoPorSector.value = _idCursoPorSector.value - sector.id
                _cursoPorSector.value = _cursoPorSector.value - sector.id
                onSuccess() // Navegar solo cuando el backend confirmó el borrado.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SectorViewModel", "Error eliminando sector", e)
                errorGeneral = mensajeErrorSector(e, "eliminar")
            } finally {
                cargando = false
            }
        }
    }

    // =====================================
    // FORMULARIO CULTIVO
    // =====================================

    var nombreCultivo by mutableStateOf("")
    var descripcionCultivo by mutableStateOf("")
    var fechaSiembra by mutableStateOf("")
    var fechaCosecha by mutableStateOf("")

    var errorNombreCultivo by mutableStateOf<String?>(null)
    var errorFechasCultivo by mutableStateOf<String?>(null)

    // =====================================
    // REGISTRAR CULTIVO EN MARIADB
    // =====================================

    fun registrarCultivo(
        sectorId: Int,
        onSuccess: () -> Unit
    ) {
        if (cargando) return

        errorNombreCultivo = null
        errorFechasCultivo = null
        errorGeneral = null

        if (nombreCultivo.isBlank()) {
            errorNombreCultivo =
                "El nombre del cultivo es obligatorio."
        }

        val siembra = runCatching {
            LocalDate.parse(fechaSiembra)
        }.getOrNull()

        val cosecha = runCatching {
            LocalDate.parse(fechaCosecha)
        }.getOrNull()

        if (siembra == null || cosecha == null) {
            errorFechasCultivo =
                "Ingresa fechas válidas (YYYY-MM-DD)."
        } else if (cosecha.isBefore(siembra)) {
            errorFechasCultivo =
                "La cosecha no puede ser anterior a la siembra."
        }

        if (
            errorNombreCultivo != null ||
            errorFechasCultivo != null
        ) {
            return
        }

        viewModelScope.launch {
            cargando = true

            try {
                val request = CrearCultivoRequestDto(
                    sectorId = sectorId.toLong(),
                    nombreComun = nombreCultivo.trim(),
                    descripcion = descripcionCultivo.trim(),
                    fechaSiembra = fechaSiembra,
                    fechaCosechaEstimada = fechaCosecha,
                    frecuenciaRiego = "Diario",
                    estadoDesarrollo = "CRECIMIENTO"
                )

                val creado =
                    RetrofitClient.cultivoApi.crear(
                        token = tokenAutenticado(),
                        cultivo = request
                    )

                _cultivos.value =
                    _cultivos.value
                        .filterNot { it.id == creado.id.toInt() } +
                            creado.aCultivoLocal()

                nombreCultivo = ""
                descripcionCultivo = ""
                fechaSiembra = ""
                fechaCosecha = ""

                onSuccess()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    "SectorViewModel",
                    "Error registrando cultivo",
                    e
                )

                errorGeneral =
                    "No se pudo registrar el cultivo: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }
}
