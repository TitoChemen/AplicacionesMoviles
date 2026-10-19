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
    version = 4, // Incrementado para recrear la BD con los nuevos datos de prueba
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
                        // Sectores y cultivo de prueba (las tareas los referencian)
                        database.sectorDao().insertarSector(
                            Sector(id = 1, codigo = "SEC-001", nombre = "Huerto Principal",
                                descripcion = "Almácigos y hortalizas de hoja", ubicacion = "Patio norte",
                                estado = "ACTIVO", fechaCreacion = "2026-03-01", responsableId = "admin1")
                        )
                        database.sectorDao().insertarSector(
                            Sector(id = 2, codigo = "SEC-002", nombre = "Huerto Secundario",
                                descripcion = "Lechugas y hierbas", ubicacion = "Patio sur",
                                estado = "ACTIVO", fechaCreacion = "2026-03-01", responsableId = "admin1")
                        )
                        database.cultivoDao().insertarCultivo(
                            Cultivo(id = 1, sectorId = 1, nombreComun = "Tomate Cherry",
                                descripcion = "Almácigos de tomate", fechaSiembra = "2026-03-01",
                                fechaCosechaEstimada = "2026-06-01", frecuenciaRiego = "Diario",
                                estadoDesarrollo = "CRECIMIENTO")
                        )

                        // Precargamos un usuario Admin y un Estudiante por defecto para las pruebas
                        database.usuarioDao().insertarUsuario(
                            Usuario("admin1", "Profesor Juan", "ADMINISTRADOR", "Taller de Huerto", "ACTIVA")
                        )
                        database.usuarioDao().insertarUsuario(
                            Usuario("estudiante1", "Kevin (Tú)", "ESTUDIANTE", "8vo Básico", "ACTIVA")
                        )

                        // Tareas de prueba asignadas a estudiante1
                        database.tareaDao().insertarTarea(
                            Tarea(
                                tipo = "RIEGO",
                                descripcion = "Regar los almácigos de tomates y revisar humedad de la tierra.",
                                sectorId = 1,
                                cultivoId = 1,
                                fechaProgramada = "Hoy - 10:30 AM",
                                asignadoA = "estudiante1",
                                estado = "PENDIENTE"
                            )
                        )
                        database.tareaDao().insertarTarea(
                            Tarea(
                                tipo = "SIEMBRA",
                                descripcion = "Plantar semillas de lechuga escarola a 15 cm de distancia.",
                                sectorId = 2,
                                cultivoId = null,
                                fechaProgramada = "Hoy - 12:00 PM",
                                asignadoA = "estudiante1",
                                estado = "PENDIENTE"
                            )
                        )

                    }
                }
            }
        }
    }
}