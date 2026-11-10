
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

class SectorViewModel(application: Application) :
    AndroidViewModel(application) {

    private val _sectores =
        MutableStateFlow<List<Sector>>(emptyList())
    val sectores = _sectores.asStateFlow()

    private val _cultivos =
        MutableStateFlow<List<Cultivo>>(emptyList())
    val cultivos = _cultivos.asStateFlow()

    private var accessToken: String? = null

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
                    responsableId = responsableId
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
    // ELIMINAR SECTOR DE MARIADB
    // =====================================

    fun eliminarSector(sector: Sector) {
        if (cargando) return

        viewModelScope.launch {
            cargando = true
            errorGeneral = null

            try {
                RetrofitClient.sectorApi.eliminar(
                    token = tokenAutenticado(),
                    id = sector.id.toLong()
                )

                _sectores.value =
                    _sectores.value.filterNot {
                        it.id == sector.id
                    }

                // Recargar datos desde MariaDB
                val token = tokenAutenticado()

                _sectores.value =
                    RetrofitClient.sectorApi
                        .listar(token)
                        .map { it.aSectorLocal() }

                _cultivos.value =
                    RetrofitClient.cultivoApi
                        .listar(token)
                        .map { it.aCultivoLocal() }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    "SectorViewModel",
                    "Error eliminando sector",
                    e
                )

                errorGeneral =
                    "No se pudo eliminar el sector: ${e.message}"
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
