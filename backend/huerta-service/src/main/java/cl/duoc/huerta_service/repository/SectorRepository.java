package cl.duoc.huerta_service.repository;

import cl.duoc.huerta_service.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository
        extends JpaRepository<Sector, Long> {

    boolean existsByCodigo(String codigo);
}
