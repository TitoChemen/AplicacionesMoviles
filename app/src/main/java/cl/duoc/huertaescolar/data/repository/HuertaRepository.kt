package cl.duoc.huertaescolar.data.repository

import cl.duoc.huertaescolar.data.local.dao.*
import cl.duoc.huertaescolar.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class HuertaRepository(
    private val usuarioDao: UsuarioDao,
    private val sectorDao: SectorDao,
    private val cultivoDao: CultivoDao,
    private val tareaDao: TareaDao,
    private val alertaDao: AlertaDao
) {
    // Usuarios / Login
    suspend fun login(id: String): Usuario? = usuarioDao.obtenerUsuarioPorId(id)

    // Sectores y Cultivos (Para que los consuma el Oscar)
    val sectores: Flow<List<Sector>> = sectorDao.obtenerSectores()

    fun obtenerCultivosDelSector(sectorId: Int): Flow<List<Cultivo>> =
        cultivoDao.obtenerCultivosPorSector(sectorId)

    suspend fun insertarSector(sector: Sector) = sectorDao.insertarSector(sector)
    suspend fun insertarCultivo(cultivo: Cultivo) = cultivoDao.insertarCultivo(cultivo)

    // Tareas (Para que los consuma el Jaime)
    // Métodos para el módulo de Tareas y Alertas dentro de HuertaRepository.kt
    fun obtenerTareasPendientes(estudianteId: String) =
        tareaDao.obtenerTareasPendientesEstudiante(estudianteId)

    fun obtenerHistorialTareas(estudianteId: String) =
        tareaDao.obtenerHistorialTareasEstudiante(estudianteId)

    fun obtenerHistorialAlertas() =
        alertaDao.obtenerHistorialAlertas()

    suspend fun marcarTareaComoRealizada(
        tarea: Tarea,
        comentarioEstudiante: String?,
        rutaFoto: String
    ) {
        val tareaActualizada = tarea.copy(
            estado = "REALIZADA", // O "PENDIENTE_VALIDACION" si requiere revisión docente
            comentarioEstudiante = comentarioEstudiante?.ifBlank { null },
            rutaFotografia = rutaFoto
        )
        tareaDao.actualizarTarea(tareaActualizada)
    }

    suspend fun registrarAlerta(alerta: Alerta) {
        alertaDao.insertarAlerta(alerta)
    }

    suspend fun insertarTarea(tarea: Tarea) {
        tareaDao.insertarTarea(tarea)
    }
}