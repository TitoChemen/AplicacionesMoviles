package cl.duoc.huertaescolar.data.repository

import cl.duoc.huertaescolar.data.local.dao.*
import cl.duoc.huertaescolar.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class HuertaRepository(
    private val usuarioDao: UsuarioDao,
    private val sectorDao: SectorDao,
    private val cultivoDao: CultivoDao,
    private val tareaDao: TareaDao
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
    fun obtenerTareasEstudiante(estudianteId: String): Flow<List<Tarea>> =
        tareaDao.obtenerTareasPorEstudiante(estudianteId)

    suspend fun insertarTarea(tarea: Tarea) = tareaDao.insertarTarea(tarea)
    suspend fun actualizarTarea(id: Int, estado: String, comentario: String) =
        tareaDao.actualizarEstadoTarea(id, estado, comentario)
}