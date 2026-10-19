package cl.duoc.huertaescolar.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cl.duoc.huertaescolar.data.local.dao.*
import cl.duoc.huertaescolar.data.local.entity.*

@Database(
    entities = [
        Usuario::class,
        Sector::class,
        Cultivo::class,
        Tarea::class,
        Alerta::class
    ],
    version = 5, // Incrementado para recrear la BD limpia
    exportSchema = false
)
abstract class HuertaDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun sectorDao(): SectorDao
    abstract fun cultivoDao(): CultivoDao
    abstract fun tareaDao(): TareaDao
    abstract fun alertaDao(): AlertaDao

    companion object {
        @Volatile
        private var INSTANCE: HuertaDatabase? = null

        fun obtenerBaseDatos(context: Context): HuertaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HuertaDatabase::class.java,
                    "huerta_escolar_db"
                )
                    .fallbackToDestructiveMigration() // Recrea la BD si cambia de versión
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}