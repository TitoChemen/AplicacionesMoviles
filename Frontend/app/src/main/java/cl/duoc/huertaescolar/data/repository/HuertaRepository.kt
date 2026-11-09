package cl.duoc.huertaescolar.data.repository

import cl.duoc.huertaescolar.data.local.dao.*
import cl.duoc.huertaescolar.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class HuertaRepository(
    private val sectorDao: SectorDao,
    private val cultivoDao: CultivoDao,
    private val tareaDao: TareaDao,
    private val usuarioDao: UsuarioDao,
    private val alertaDao: AlertaDao
) {
    // 🔐 Auth / Usuarios con validación de contraseña segura
    suspend fun login(id: String, contrasenaIngresada: String): Usuario? {
        precargarDatosDePrueba() // Garantiza que admin1 y estudiante1 existan
        val usuario = usuarioDao.obtenerUsuarioPorId(id)

        // Validamos si el usuario existe y si su contraseña coincide
        if (usuario != null && usuario.contrasena == contrasenaIngresada) {
            return usuario
        }
        return null // Si falla la clave o no existe, retorna nulo
    }

    /**
     * Inserta usuarios, sectores, cultivo y tareas de prueba con sus respectivas contraseñas.
     */
    suspend fun precargarDatosDePrueba() {
        usuarioDao.insertarUsuario(
            Usuario(
                identificador = "admin1",
                nombreFicticio = "Profesor Juan",
                perfil = "ADMINISTRADOR",
                tallerAsignado = "Taller de Huerto",
                estadoCuenta = "ACTIVA",
                contrasena = "admin123" // 🔒 Clave segura del Administrador
            )
        )
        usuarioDao.insertarUsuario(
            Usuario(
                identificador = "estudiante1",
                nombreFicticio = "Kevin (Tú)",
                perfil = "ESTUDIANTE",
                tallerAsignado = "8vo Básico",
                estadoCuenta = "ACTIVA",
                contrasena = "alumno123" // 🔒 Clave segura del Estudiante
            )
        )

        if (tareaDao.obtenerTodasLasTareas().isEmpty()) {
            sectorDao.insertarSector(
                Sector(id = 1, codigo = "SEC-001", nombre = "Huerto Principal",
                    descripcion = "Almácigos y hortalizas de hoja", ubicacion = "Patio norte",
                    estado = "ACTIVO", fechaCreacion = "2026-03-01", responsableId = "admin1")
            )
            sectorDao.insertarSector(
                Sector(id = 2, codigo = "SEC-002", nombre = "Huerto Secundario",
                    descripcion = "Lechugas y hierbas", ubicacion = "Patio sur",
                    estado = "ACTIVO", fechaCreacion = "2026-03-01", responsableId = "admin1")
            )
            cultivoDao.insertarCultivo(
                Cultivo(id = 1, sectorId = 1, nombreComun = "Tomate Cherry",
                    descripcion = "Almácigos de tomate", fechaSiembra = "2026-03-01",
                    fechaCosechaEstimada = "2026-06-01", frecuenciaRiego = "Diario",
                    estadoDesarrollo = "CRECIMIENTO")
            )
            tareaDao.insertarTarea(
                Tarea(tipo = "RIEGO",
                    descripcion = "Regar los almácigos de tomates y revisar humedad de la tierra.",
                    sectorId = 1, cultivoId = 1, fechaProgramada = "Hoy - 10:30 AM",
                    asignadoA = "estudiante1", estado = "PENDIENTE")
            )
            tareaDao.insertarTarea(
                Tarea(tipo = "SIEMBRA",
                    descripcion = "Plantar semillas de lechuga escarola a 15 cm de distancia.",
                    sectorId = 2, cultivoId = null, fechaProgramada = "Hoy - 12:00 PM",
                    asignadoA = "estudiante1", estado = "PENDIENTE")
            )
        }
    }

    suspend fun obtenerUsuarios(): List<Usuario> = usuarioDao.obtenerUsuarios()

    // Sectores
    fun obtenerSectores(): Flow<List<Sector>> = sectorDao.obtenerSectores()
    suspend fun insertarSector(sector: Sector) = sectorDao.insertarSector(sector)

    // Cultivos
    fun obtenerCultivos(): Flow<List<Cultivo>> = cultivoDao.obtenerCultivos()
    fun obtenerCultivosDelSector(sectorId: Int): Flow<List<Cultivo>> =
        cultivoDao.obtenerCultivosPorSector(sectorId)
    suspend fun insertarCultivo(cultivo: Cultivo) = cultivoDao.insertarCultivo(cultivo)

    // Tareas
    fun obtenerTareasPendientes(estudianteId: String) =
        tareaDao.obtenerTareasPendientesEstudiante(estudianteId)

    fun obtenerHistorialTareas(estudianteId: String) =
        tareaDao.obtenerHistorialTareasEstudiante(estudianteId)

    fun obtenerTareasDelEstudiante(estudianteId: String): Flow<List<Tarea>> =
        tareaDao.obtenerTareasEstudiante(estudianteId)

    fun obtenerTodasLasTareasFlow(): Flow<List<Tarea>> = tareaDao.obtenerTodasLasTareasFlow()

    suspend fun obtenerTodasLasTareas(): List<Tarea> = tareaDao.obtenerTodasLasTareas()

    suspend fun eliminarTarea(tarea: Tarea) {
        tareaDao.eliminarTarea(tarea)
    }

    suspend fun insertarTarea(tarea: Tarea) {
        tareaDao.insertarTarea(tarea)
    }

    suspend fun actualizarTarea(tarea: Tarea) {
        tareaDao.actualizarTarea(tarea)
    }

    suspend fun eliminarSector(sector: Sector) {
        sectorDao.eliminarSector(sector)
    }

    suspend fun marcarTareaComoRealizada(
        tarea: Tarea,
        comentarioEstudiante: String?,
        rutaFoto: String
    ) {
        val tareaActualizada = tarea.copy(
            estado = "REALIZADA",
            comentarioEstudiante = comentarioEstudiante?.ifBlank { null },
            rutaFotografia = rutaFoto
        )
        tareaDao.actualizarTarea(tareaActualizada)
    }

    // Alertas
    fun obtenerHistorialAlertas() = alertaDao.obtenerHistorialAlertas()

    suspend fun registrarAlerta(alerta: Alerta) {
        alertaDao.insertarAlerta(alerta)
    }
}