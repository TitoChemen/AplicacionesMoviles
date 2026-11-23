package cl.duoc.huertaescolar

import cl.duoc.huertaescolar.data.local.entity.*
import org.junit.Assert.*
import org.junit.Test

class EntidadesTest {

    @Test
    fun crearAlertaConValoresPorDefecto() {
        val alerta = Alerta(
            id = 1,
            tipoProblema = "PLAGA",
            descripcion = "Plaga de pulgones en tomates",
            sectorId = 2,
            fechaReporte = "2026-03-01",
            reportadoPor = "estudiante1"
        )
        assertEquals(1, alerta.id)
        assertEquals("PLAGA", alerta.tipoProblema)
        assertEquals("Plaga de pulgones en tomates", alerta.descripcion)
        assertEquals(2, alerta.sectorId)
        assertEquals("2026-03-01", alerta.fechaReporte)
        assertEquals("estudiante1", alerta.reportadoPor)
        assertEquals("PENDIENTE", alerta.estado)
        assertNull(alerta.rutaFotografia)
    }

    @Test
    fun crearCultivoConParametros() {
        val cultivo = Cultivo(
            id = 10,
            sectorId = 1,
            nombreComun = "Tomate Cherry",
            descripcion = "Siembra de tomates",
            fechaSiembra = "2026-03-01",
            fechaCosechaEstimada = "2026-06-01",
            frecuenciaRiego = "Diario",
            estadoDesarrollo = "CRECIMIENTO"
        )
        assertEquals(10, cultivo.id)
        assertEquals(1, cultivo.sectorId)
        assertEquals("Tomate Cherry", cultivo.nombreComun)
        assertEquals("Siembra de tomates", cultivo.descripcion)
        assertEquals("2026-03-01", cultivo.fechaSiembra)
        assertEquals("2026-06-01", cultivo.fechaCosechaEstimada)
        assertEquals("Diario", cultivo.frecuenciaRiego)
        assertEquals("CRECIMIENTO", cultivo.estadoDesarrollo)
    }

    @Test
    fun crearSectorConParametros() {
        val sector = Sector(
            id = 5,
            codigo = "SEC-001",
            nombre = "Huerto Principal",
            descripcion = "Sector de hortalizas",
            ubicacion = "Patio norte",
            estado = "ACTIVO",
            fechaCreacion = "2026-03-01",
            responsableId = "admin1"
        )
        assertEquals(5, sector.id)
        assertEquals("SEC-001", sector.codigo)
        assertEquals("Huerto Principal", sector.nombre)
        assertEquals("Sector de hortalizas", sector.descripcion)
        assertEquals("Patio norte", sector.ubicacion)
        assertEquals("ACTIVO", sector.estado)
        assertEquals("2026-03-01", sector.fechaCreacion)
        assertEquals("admin1", sector.responsableId)
    }

    @Test
    fun crearTareaConParametros() {
        val tarea = Tarea(
            id = 3,
            tipo = "RIEGO",
            descripcion = "Regar almácigos",
            sectorId = 1,
            cultivoId = 10,
            fechaProgramada = "2026-03-01",
            asignadoA = "estudiante1",
            estado = "PENDIENTE"
        )
        assertEquals(3, tarea.id)
        assertEquals("RIEGO", tarea.tipo)
        assertEquals("Regar almácigos", tarea.descripcion)
        assertEquals(1, tarea.sectorId)
        assertEquals(10, tarea.cultivoId)
        assertEquals("2026-03-01", tarea.fechaProgramada)
        assertEquals("estudiante1", tarea.asignadoA)
        assertEquals("PENDIENTE", tarea.estado)
        assertNull(tarea.comentarioEstudiante)
        assertNull(tarea.comentarioDocente)
        assertNull(tarea.rutaFotografia)
    }

    @Test
    fun crearUsuarioConParametros() {
        val usuario = Usuario(
            identificador = "estudiante1",
            nombreFicticio = "Kevin",
            perfil = "ESTUDIANTE",
            tallerAsignado = "8vo Básico",
            estadoCuenta = "ACTIVA",
            contrasena = "alumno123",
            backendId = 99L
        )
        assertEquals("estudiante1", usuario.identificador)
        assertEquals("Kevin", usuario.nombreFicticio)
        assertEquals("ESTUDIANTE", usuario.perfil)
        assertEquals("8vo Básico", usuario.tallerAsignado)
        assertEquals("ACTIVA", usuario.estadoCuenta)
        assertEquals("alumno123", usuario.contrasena)
        assertEquals(99L, usuario.backendId)
    }
}
