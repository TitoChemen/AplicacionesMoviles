package cl.duoc.huertaescolar.data.local.dao

import androidx.room.*
import cl.duoc.huertaescolar.data.local.entity.Alerta
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertaDao {

    @Query("SELECT * FROM alertas ORDER BY id DESC")
    fun obtenerHistorialAlertas(): Flow<List<Alerta>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAlerta(alerta: Alerta)
}