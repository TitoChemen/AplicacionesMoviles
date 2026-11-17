package cl.duoc.huerta_service.service;

import cl.duoc.huerta_service.entity.Cultivo;
import cl.duoc.huerta_service.security.CursoActual;
import cl.duoc.huerta_service.exception.*;
import cl.duoc.huerta_service.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CultivoService {

    private final CultivoRepository cultivoRepository;
    private final SectorRepository sectorRepository;
    private final SectorService sectorService;
    private final CursoActual acceso;

    public CultivoService(
            CultivoRepository cultivoRepository,
            SectorRepository sectorRepository, SectorService sectorService, CursoActual acceso) {
        this.cultivoRepository = cultivoRepository;
        this.sectorRepository = sectorRepository;
        this.sectorService = sectorService;
        this.acceso = acceso;
    }

    public List<Cultivo> listar() {
        var ids = (acceso.esAdminGlobal() ? sectorRepository.findAll()
                : sectorRepository.findByCursoId(acceso.id())).stream()
                .map(s -> s.getId()).toList();
        return ids.isEmpty() ? List.of() : cultivoRepository.findBySectorIdIn(ids);
    }

    public List<Cultivo> listarPorSector(Long sectorId) {
        verificarSector(sectorId);
        return cultivoRepository.findBySectorId(sectorId);
    }

    public Cultivo buscar(Long id) {
        Cultivo cultivo = cultivoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cultivo no encontrado: " + id));
        verificarSector(cultivo.getSectorId());
        return cultivo;
    }

    @Transactional
    public Cultivo crear(Cultivo cultivo) {
        acceso.exigirDocente();
        validar(cultivo);
        if (!"ACTIVO".equalsIgnoreCase(sectorService.buscar(cultivo.getSectorId()).getEstado())) {
            throw new ConflictoException("El sector está archivado; actívalo antes de crear cultivos");
        }
        cultivo.setId(null);
        return cultivoRepository.save(cultivo);
    }

    @Transactional
    public Cultivo actualizar(Long id, Cultivo datos) {
        acceso.exigirDocente();
        Cultivo actual = buscar(id);
        validar(datos);

        actual.setSectorId(datos.getSectorId());
        actual.setNombreComun(datos.getNombreComun());
        actual.setDescripcion(datos.getDescripcion());
        actual.setFechaSiembra(datos.getFechaSiembra());
        actual.setFechaCosechaEstimada(
                datos.getFechaCosechaEstimada());
        actual.setFrecuenciaRiego(datos.getFrecuenciaRiego());
        actual.setEstadoDesarrollo(datos.getEstadoDesarrollo());

        return cultivoRepository.save(actual);
    }

    @Transactional
    public void eliminar(Long id) {
        acceso.exigirDocente();
        Cultivo cultivo = buscar(id);
        cultivoRepository.delete(cultivo);
    }

    private void verificarSector(Long id) {
        if (id == null) throw new RecursoNoEncontradoException("Sector inexistente");
        sectorService.buscar(id); // tambien valida curso
    }

    private void validar(Cultivo cultivo) {
        verificarSector(cultivo.getSectorId());

        if (cultivo.getNombreComun() == null ||
                cultivo.getNombreComun().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del cultivo es obligatorio");
        }
    }
}
