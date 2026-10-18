package cl.duoc.huertaescolar.data.local.dao

import androidx.room.*
import cl.duoc.huertaescolar.data.local.entity.Tarea
import cl.duoc.huertaescolar.data.local.entity.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {
    /*
    ANTERIORES
    @Query("SELECT * FROM tareas WHERE asignadoA = :estudianteId")
    fun obtenerTareasPorEstudiante(estudianteId: String): Flow<List<Tarea>>

    @Query("UPDATE tareas SET estado = :nuevoEstado, comentarioEstudiante = :comentario WHERE id = :tareaId")
    suspend fun actualizarEstadoTarea(tareaId: Int, nuevoEstado: String, comentario: String)
    */

    @Query("SELECT * FROM tareas WHERE asignadoA = :estudianteId AND estado = 'PENDIENTE' ORDER BY id DESC")
    fun obtenerTareasPendientesEstudiante(estudianteId: String): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas WHERE asignadoA = :estudianteId AND estado != 'PENDIENTE' ORDER BY id DESC")
    fun obtenerHistorialTareasEstudiante(estudianteId: String): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas")
    suspend fun obtenerTodasLasTareas(): List<Tarea>

    @Query("SELECT * FROM tareas") // Cambia "tabla_usuarios" por el nombre real de tu tabla
    suspend fun obtenerUsuarios(): List<Usuario>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTarea(tarea: Tarea)

    @Update
    suspend fun actualizarTarea(tarea: Tarea)
}