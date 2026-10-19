package cl.duoc.huertaescolar.data.repository

import cl.duoc.huertaescolar.data.local.dao.*
import cl.duoc.huertaescolar.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class HuertaRepository(
    private val sectorDao: SectorDao,
    private val cultivoDao: CultivoDao,
    private val tareaDao: TareaDao,
    private val usuarioDao: UsuarioDao,
    private val alertaDao: AlertaDao
) {
    // Auth / Usuarios
    suspend fun login(id: String): Usuario? = usuarioDao.obtenerUsuarioPorId(id)

    suspend fun obtenerUsuarios(): List<Usuario> = usuarioDao.obtenerUsuarios()

    // Sectores
    fun obtenerSectores(): Flow<List<Sector>> = sectorDao.obtenerSectores()
    suspend fun insertarSector(sector: Sector) = sectorDao.insertarSector(sector)

    // Cultivos
    fun obtenerCultivos(): Flow<List<Cultivo>> = cultivoDao.obtenerCultivos()
    fun obtenerCultivosDelSector(sectorId: Int): Flow<List<Cultivo>> =
        cultivoDao.obtenerCultivosPorSector(sectorId)
    suspend fun insertarCultivo(cultivo: Cultivo) = cultivoDao.insertarCultivo(cultivo)

    // Tareas
    fun obtenerTareasPendientes(estudianteId: String) =
        tareaDao.obtenerTareasPendientesEstudiante(estudianteId)

    fun obtenerHistorialTareas(estudianteId: String) =
        tareaDao.obtenerHistorialTareasEstudiante(estudianteId)

    /** Todas las tareas de un estudiante (pendientes + historial), en tiempo real. */
    fun obtenerTareasDelEstudiante(estudianteId: String): Flow<List<Tarea>> =
        tareaDao.obtenerTareasEstudiante(estudianteId)

    /** Todas las tareas (vista del administrador), en tiempo real. */
    fun obtenerTodasLasTareasFlow(): Flow<List<Tarea>> = tareaDao.obtenerTodasLasTareasFlow()

    suspend fun obtenerTodasLasTareas(): List<Tarea> = tareaDao.obtenerTodasLasTareas()

    suspend fun insertarTarea(tarea: Tarea) {
        tareaDao.insertarTarea(tarea)
    }

    suspend fun actualizarTarea(tarea: Tarea) {
        tareaDao.actualizarTarea(tarea)
    }

    suspend fun marcarTareaComoRealizada(
        tarea: Tarea,
        comentarioEstudiante: String?,
        rutaFoto: String
    ) {
        val tareaActualizada = tarea.copy(
            estado = "REALIZADA",
            comentarioEstudiante = comentarioEstudiante?.ifBlank { null },
            rutaFotografia = rutaFoto
        )
        tareaDao.actualizarTarea(tareaActualizada)
    }

    // Alertas
    fun obtenerHistorialAlertas() = alertaDao.obtenerHistorialAlertas()

    suspend fun registrarAlerta(alerta: Alerta) {
        alertaDao.insertarAlerta(alerta)
    }
}
