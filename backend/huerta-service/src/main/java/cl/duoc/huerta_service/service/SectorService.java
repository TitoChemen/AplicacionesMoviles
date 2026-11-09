package cl.duoc.huerta_service.service;

import cl.duoc.huerta_service.entity.Sector;
import cl.duoc.huerta_service.exception.*;
import cl.duoc.huerta_service.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;
    private final CultivoRepository cultivoRepository;

    public SectorService(
            SectorRepository sectorRepository,
            CultivoRepository cultivoRepository) {
        this.sectorRepository = sectorRepository;
        this.cultivoRepository = cultivoRepository;
    }

    public List<Sector> listar() {
        return sectorRepository.findAll();
    }

    public Sector buscar(Long id) {
        return sectorRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Sector no encontrado: " + id));
    }

    @Transactional
    public Sector crear(Sector sector) {
        validar(sector);

        if (sectorRepository.existsByCodigo(sector.getCodigo())) {
            throw new ConflictoException(
                    "El código del sector ya existe");
        }

        sector.setId(null);
        return sectorRepository.save(sector);
    }

    @Transactional
    public Sector actualizar(Long id, Sector datos) {
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
        actual.setFechaCreacion(datos.getFechaCreacion());
        actual.setResponsableId(datos.getResponsableId());

        return sectorRepository.save(actual);
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);

        if (cultivoRepository.existsBySectorId(id)) {
            throw new ConflictoException(
                    "No puedes eliminar un sector con cultivos");
        }

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
