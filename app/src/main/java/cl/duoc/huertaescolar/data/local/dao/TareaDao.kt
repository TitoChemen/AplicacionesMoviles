package cl.duoc.huertaescolar.data.local.dao

import androidx.room.*
import cl.duoc.huertaescolar.data.local.entity.Tarea
import cl.duoc.huertaescolar.data.local.entity.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas WHERE asignadoA = :estudianteId AND estado = 'PENDIENTE' ORDER BY id DESC")
    fun obtenerTareasPendientesEstudiante(estudianteId: String): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas WHERE asignadoA = :estudianteId AND estado != 'PENDIENTE' ORDER BY id DESC")
    fun obtenerHistorialTareasEstudiante(estudianteId: String): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas WHERE asignadoA = :estudianteId ORDER BY id DESC")
    fun obtenerTareasEstudiante(estudianteId: String): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas ORDER BY id DESC")
    fun obtenerTodasLasTareasFlow(): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas")
    suspend fun obtenerTodasLasTareas(): List<Tarea>

    @Query("SELECT * FROM usuarios")
    suspend fun obtenerUsuarios(): List<Usuario>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTarea(tarea: Tarea)

    @Update
    suspend fun actualizarTarea(tarea: Tarea)

    @Delete
    suspend fun eliminarTarea(tarea: Tarea) // <-- ¡Esta es la que faltaba para borrar tareas!
}