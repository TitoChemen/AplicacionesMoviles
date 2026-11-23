package cl.duoc.huertaescolar

import cl.duoc.huertaescolar.data.EstadoTarea
import cl.duoc.huertaescolar.data.Perfil
import org.junit.Assert.*
import org.junit.Test

class ConstantesTest {

    @Test
    fun perfilEsPersonalRetornaTrueParaAdminYDocente() {
        assertTrue(Perfil.esPersonal(Perfil.ADMINISTRADOR))
        assertTrue(Perfil.esPersonal(Perfil.DOCENTE))
        assertFalse(Perfil.esPersonal(Perfil.ESTUDIANTE))
        assertFalse(Perfil.esPersonal(null))
        assertFalse(Perfil.esPersonal("OTRO"))
    }

    @Test
    fun estadoTareaEtiquetaRetornaTextoCorrecto() {
        assertEquals("Todas", EstadoTarea.etiqueta(EstadoTarea.TODAS))
        assertEquals("Pendiente", EstadoTarea.etiqueta(EstadoTarea.PENDIENTE))
        assertEquals("Pendiente de validación", EstadoTarea.etiqueta(EstadoTarea.PENDIENTE_VALIDACION))
        assertEquals("Validada", EstadoTarea.etiqueta(EstadoTarea.VALIDADA))
        assertEquals("Rechazada", EstadoTarea.etiqueta(EstadoTarea.RECHAZADA))
        assertEquals("DESCONOCIDO", EstadoTarea.etiqueta("DESCONOCIDO"))
    }

    @Test
    fun estadoTareaFiltrosContieneTodosLosEstados() {
        val filtros = EstadoTarea.filtros
        assertEquals(5, filtros.size)
        assertTrue(filtros.contains(EstadoTarea.TODAS))
        assertTrue(filtros.contains(EstadoTarea.PENDIENTE))
        assertTrue(filtros.contains(EstadoTarea.PENDIENTE_VALIDACION))
        assertTrue(filtros.contains(EstadoTarea.VALIDADA))
        assertTrue(filtros.contains(EstadoTarea.RECHAZADA))
    }
}
