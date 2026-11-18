package cl.duoc.huerta_service.service;

import cl.duoc.huerta_service.entity.Sector;
import cl.duoc.huerta_service.exception.ConflictoException;
import cl.duoc.huerta_service.repository.CultivoRepository;
import cl.duoc.huerta_service.repository.SectorRepository;
import cl.duoc.huerta_service.security.CursoActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SectorServiceTest {
    private final SectorRepository sectores = mock(SectorRepository.class);
    private final CultivoRepository cultivos = mock(CultivoRepository.class);
    private final CursoActual acceso = mock(CursoActual.class);
    private final CursoReferenciaService cursos = mock(CursoReferenciaService.class);
    private final SectorDependenciasService dependencias = mock(SectorDependenciasService.class);
    private SectorService service;

    @BeforeEach
    void iniciar() {
        service = new SectorService(sectores, cultivos, acceso, cursos, dependencias);
        when(sectores.save(any(Sector.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Sector sector(Long id, Long cursoId) {
        Sector s = new Sector();
        s.setId(id);
        s.setCodigo("SEC-001");
        s.setNombre("Huerta");
        s.setUbicacion("Patio");
        s.setEstado("ACTIVO");
        s.setCursoId(cursoId);
        return s;
    }

    @Test
    void adminPuedeTrasladarSectorSinTareasNiAlertas() {
        Sector actual = sector(1L, 7L);
        when(sectores.findById(1L)).thenReturn(Optional.of(actual));
        when(acceso.usuarioId()).thenReturn(2L);
        Sector resultado = service.actualizar(1L, sector(null, 8L));
        assertEquals(8L, resultado.getCursoId());
        verify(cursos).exigirCursoExistente(8L);
        verify(dependencias).exigirSinTareasNiAlertas(1L);
    }

    @Test
    void rechazaTrasladoConTareasYNoModificaCurso() {
        Sector actual = sector(1L, 7L);
        when(sectores.findById(1L)).thenReturn(Optional.of(actual));
        doThrow(new ConflictoException("Tiene tareas"))
                .when(dependencias).exigirSinTareasNiAlertas(1L);
        assertThrows(ConflictoException.class,
                () -> service.actualizar(1L, sector(null, 8L)));
        assertEquals(7L, actual.getCursoId());
        verify(sectores, never()).save(actual);
    }

    @Test
    void editarSinCambiarCursoNoConsultaDependencias() {
        when(sectores.findById(1L)).thenReturn(Optional.of(sector(1L, 7L)));
        service.actualizar(1L, sector(null, 7L));
        verifyNoInteractions(dependencias);
    }

    @Test
    void docenteNoPuedeCrearNiEditarNiEliminarSector() {
        doThrow(new AccessDeniedException("Solo administrador"))
                .when(acceso).exigirAdminGlobal();
        assertThrows(AccessDeniedException.class, () -> service.crear(sector(null, 7L)));
        assertThrows(AccessDeniedException.class, () -> service.actualizar(1L, sector(null, 7L)));
        assertThrows(AccessDeniedException.class, () -> service.eliminar(1L));
        verifyNoInteractions(sectores, cultivos, cursos, dependencias);
    }

    @Test
    void crearExigeCursoRegistradoYNoUsaResponsableDelCliente() {
        Sector datos = sector(99L, 7L);
        datos.setResponsableId("falsificado");
        when(acceso.usuarioId()).thenReturn(42L);
        Sector resultado = service.crear(datos);
        assertNull(resultado.getId());
        assertEquals("42", resultado.getResponsableId());
        verify(cursos).exigirCursoExistente(7L);
    }

    @Test
    void noPermiteCrearConCursoInexistente() {
        doThrow(new IllegalArgumentException("Curso inexistente"))
                .when(cursos).exigirCursoExistente(100L);
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(sector(null, 100L)));
        verify(sectores, never()).save(any());
    }
}
