package cl.duoc.huertaescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.huertaescolar.data.local.entity.Cultivo
import kotlinx.coroutines.flow.Flow

@Dao
interface CultivoDao {
    // Agrega esta línea para obtener todos los cultivos
    @Query("SELECT * FROM cultivos")
    fun obtenerCultivos(): Flow<List<Cultivo>>

    @Query("SELECT * FROM cultivos WHERE sectorId = :sectorId")
    fun obtenerCultivosPorSector(sectorId: Int): Flow<List<Cultivo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCultivo(cultivo: Cultivo)
}