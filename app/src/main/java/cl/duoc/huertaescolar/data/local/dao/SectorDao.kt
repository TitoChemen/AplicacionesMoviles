package cl.duoc.huertaescolar.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.huertaescolar.data.local.entity.Sector
import kotlinx.coroutines.flow.Flow

@Dao
interface SectorDao {
    @Query("SELECT * FROM sectores")
    fun obtenerSectores(): Flow<List<Sector>> // Usamos Flow para que la UI se actualice solita en tiempo real

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSector(sector: Sector)

    @Delete
    suspend fun eliminarSector(sector: Sector)
}