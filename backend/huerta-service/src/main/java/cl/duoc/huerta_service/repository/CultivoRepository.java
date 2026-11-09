package cl.duoc.huerta_service.repository;

import cl.duoc.huerta_service.entity.Cultivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CultivoRepository
        extends JpaRepository<Cultivo, Long> {

    List<Cultivo> findBySectorId(Long sectorId);

    boolean existsBySectorId(Long sectorId);
}
