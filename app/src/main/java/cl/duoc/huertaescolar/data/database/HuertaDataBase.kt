package cl.duoc.huertaescolar.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duoc.huertaescolar.data.local.dao.*
import cl.duoc.huertaescolar.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Usuario::class, Sector::class, Cultivo::class, Tarea::class], version = 1, exportSchema = false)
abstract class HuertaDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun sectorDao(): SectorDao
    abstract fun cultivoDao(): CultivoDao
    abstract fun tareaDao(): TareaDao

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
                    .addCallback(CallbackPrecarga()) // Inserta datos iniciales al arrancar por primera vez
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class CallbackPrecarga : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        // Precargamos un usuario Admin y un Estudiante por defecto para las pruebas
                        database.usuarioDao().insertarUsuario(
                            Usuario("admin1", "Profesor Juan", "ADMINISTRADOR", "Taller de Huerto", "ACTIVA")
                        )
                        database.usuarioDao().insertarUsuario(
                            Usuario("estudiante1", "Kevin (Tú)", "ESTUDIANTE", "8vo Básico", "ACTIVA")
                        )
                    }
                }
            }
        }
    }
}