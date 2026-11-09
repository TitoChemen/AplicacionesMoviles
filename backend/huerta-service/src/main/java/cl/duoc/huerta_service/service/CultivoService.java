package cl.duoc.huerta_service.service;

import cl.duoc.huerta_service.entity.Cultivo;
import cl.duoc.huerta_service.exception.*;
import cl.duoc.huerta_service.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CultivoService {

    private final CultivoRepository cultivoRepository;
    private final SectorRepository sectorRepository;

    public CultivoService(
            CultivoRepository cultivoRepository,
            SectorRepository sectorRepository) {
        this.cultivoRepository = cultivoRepository;
        this.sectorRepository = sectorRepository;
    }

    public List<Cultivo> listar() {
        return cultivoRepository.findAll();
    }

    public List<Cultivo> listarPorSector(Long sectorId) {
        verificarSector(sectorId);
        return cultivoRepository.findBySectorId(sectorId);
    }

    public Cultivo buscar(Long id) {
        return cultivoRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Cultivo no encontrado: " + id));
    }

    @Transactional
    public Cultivo crear(Cultivo cultivo) {
        validar(cultivo);
        cultivo.setId(null);
        return cultivoRepository.save(cultivo);
    }

    @Transactional
    public Cultivo actualizar(Long id, Cultivo datos) {
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
        Cultivo cultivo = buscar(id);
        cultivoRepository.delete(cultivo);
    }

    private void verificarSector(Long id) {
        if (id == null || !sectorRepository.existsById(id)) {
            throw new RecursoNoEncontradoException(
                    "El sector no existe: " + id);
        }
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
