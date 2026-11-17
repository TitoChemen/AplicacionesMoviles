package cl.duoc.huerta_service.service;

import cl.duoc.huerta_service.entity.Sector;
import cl.duoc.huerta_service.security.CursoActual;
import cl.duoc.huerta_service.exception.*;
import cl.duoc.huerta_service.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;
    private final CultivoRepository cultivoRepository;
    private final CursoActual acceso;
    private final CursoReferenciaService cursos;
    private final SectorDependenciasService dependencias;

    public SectorService(
            SectorRepository sectorRepository,
            CultivoRepository cultivoRepository, CursoActual acceso,
            CursoReferenciaService cursos, SectorDependenciasService dependencias) {
        this.sectorRepository = sectorRepository;
        this.cultivoRepository = cultivoRepository;
        this.acceso = acceso;
        this.cursos = cursos;
        this.dependencias = dependencias;
    }

    public List<Sector> listar() {
        return acceso.esAdminGlobal() ? sectorRepository.findAll()
                : sectorRepository.findByCursoId(acceso.id());
    }

    public Sector buscar(Long id) {
        Sector sector = sectorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sector no encontrado: " + id));
        acceso.exigirCurso(sector.getCursoId());
        return sector;
    }

    @Transactional
    public Sector crear(Sector sector) {
        acceso.exigirDocente(); // incluye al admin global
        validar(sector);

        if (sectorRepository.existsByCodigo(sector.getCodigo())) {
            throw new ConflictoException(
                    "El código del sector ya existe");
        }

        Long cursoDestino;
        if (acceso.esAdminGlobal()) {
            cursoDestino = sector.getCursoId();
            cursos.exigirCursoExistente(cursoDestino);
        } else {
            cursoDestino = acceso.id(); // el docente NO elige otro curso
        }
        sector.setId(null);
        sector.setCursoId(cursoDestino);
        sector.setResponsableId(acceso.usuarioId().toString());
        return sectorRepository.save(sector);
    }

    @Transactional
    public Sector actualizar(Long id, Sector datos) {
        acceso.exigirDocente();
        Sector actual = buscar(id);
        validar(datos);

        if (!actual.getCodigo().equals(datos.getCodigo())
                && sectorRepository.existsByCodigo(datos.getCodigo())) {
            throw new ConflictoException(
                    "El código del sector ya está registrado");
        }

        actual.setCodigo(datos.getCodigo());
        actual.setNombre(datos.getNombre());
        actual.setDescripcion(datos.getDescripcion());
        actual.setUbicacion(datos.getUbicacion());
        actual.setEstado(datos.getEstado());
        // Fecha de creación, curso y responsable son inmutables mediante PUT.

        return sectorRepository.save(actual);
    }

    @Transactional
    public void eliminar(Long id) {
        acceso.exigirDocente();
        buscar(id);

        if (cultivoRepository.existsBySectorId(id)) {
            throw new ConflictoException(
                    "No puedes eliminar un sector con cultivos. Archívalo para conservarlos.");
        }
        // Aunque no tenga cultivos, puede tener tareas o alertas con evidencia.
        dependencias.exigirSinTareasNiAlertas(id);
        sectorRepository.deleteById(id);
    }

    private void validar(Sector sector) {
        if (sector.getCodigo() == null ||
                sector.getCodigo().isBlank()) {
            throw new IllegalArgumentException(
                    "El código es obligatorio");
        }

        if (sector.getNombre() == null ||
                sector.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio");
        }

        if (sector.getEstado() == null ||
                sector.getEstado().isBlank()) {
            throw new IllegalArgumentException(
                    "El estado es obligatorio");
        }
    }
}
