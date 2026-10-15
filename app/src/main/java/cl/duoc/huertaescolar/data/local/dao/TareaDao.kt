package cl.duoc.huertaescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.huertaescolar.data.local.entity.Tarea
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas WHERE asignadoA = :estudianteId")
    fun obtenerTareasPorEstudiante(estudianteId: String): Flow<List<Tarea>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTarea(tarea: Tarea)

    @Query("UPDATE tareas SET estado = :nuevoEstado, comentarioEstudiante = :comentario WHERE id = :tareaId")
    suspend fun actualizarEstadoTarea(tareaId: Int, nuevoEstado: String, comentario: String)
}