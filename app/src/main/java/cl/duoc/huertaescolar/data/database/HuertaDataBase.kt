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

@Database(
    entities = [
        Usuario::class,
        Sector::class,
        Cultivo::class,
        Tarea::class,
        Alerta::class
    ],
    version = 6, // 🚀 Subimos a 6 para forzar la migración limpia con la contraseña
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
                    .fallbackToDestructiveMigration() // Borra y recrea la BD al cambiar de versión
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 🌱 Insertamos los usuarios iniciales con contraseña al crear la BD
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.usuarioDao().insertarUsuario(
                                        Usuario(
                                            identificador = "admin1",
                                            nombreFicticio = "Profesor Administrador",
                                            perfil = "ADMINISTRADOR",
                                            tallerAsignado = "Huerto Escolar Central",
                                            estadoCuenta = "ACTIVA",
                                            contrasena = "admin123" // 🔒 Clave segura del profe
                                        )
                                    )
                                    database.usuarioDao().insertarUsuario(
                                        Usuario(
                                            identificador = "estudiante1",
                                            nombreFicticio = "Estudiante Juan",
                                            perfil = "ESTUDIANTE",
                                            tallerAsignado = "Cultivos Urbanos",
                                            estadoCuenta = "ACTIVA",
                                            contrasena = "alumno123" // 🔒 Clave segura del alumno
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}