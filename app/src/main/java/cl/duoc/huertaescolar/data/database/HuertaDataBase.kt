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
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    // Usuarios
                                    database.usuarioDao().insertarUsuario(
                                        Usuario(
                                            identificador = "admin1",
                                            nombreFicticio = "Profesor Administrador",
                                            perfil = "ADMINISTRADOR",
                                            tallerAsignado = "Huerto Escolar Central",
                                            estadoCuenta = "ACTIVA",
                                            contrasena = "admin123"
                                        )
                                    )
                                    database.usuarioDao().insertarUsuario(
                                        Usuario(
                                            identificador = "estudiante1",
                                            nombreFicticio = "Estudiante Juan",
                                            perfil = "ESTUDIANTE",
                                            tallerAsignado = "Cultivos Urbanos",
                                            estadoCuenta = "ACTIVA",
                                            contrasena = "alumno123"
                                        )
                                    )
                                    // Sector Semilla (Para que no esté vacío)
                                    database.sectorDao().insertarSector(
                                        Sector(
                                            id = 1,
                                            codigo = "SEC-001",
                                            nombre = "Sector Invernadero A",
                                            descripcion = "Sector dedicado al cultivo de tomates y lechugas",
                                            ubicacion = "Zona Norte - Invernadero Principal",
                                            estado = "ACTIVO",
                                            fechaCreacion = "2026-10-27",
                                            responsableId = "estudiante1"
                                        )
                                    )

                                    // Tarea Semilla (Para probar la lista de inmediato)
                                    database.tareaDao().insertarTarea(
                                        Tarea(
                                            id = 1,
                                            tipo = "RIEGO",
                                            descripcion = "Regar el sector A en la mañana",
                                            sectorId = 1,
                                            cultivoId = null,
                                            fechaProgramada = "2026-10-28",
                                            asignadoA = "estudiante1",
                                            estado = "PENDIENTE"
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