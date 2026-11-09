
package cl.duoc.tareas_service.repository;

import cl.duoc.tareas_service.entity.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaRepository
        extends JpaRepository<Alerta, Long> {

    List<Alerta> findBySectorId(Long sectorId);

    List<Alerta> findByEstado(String estado);

    List<Alerta> findByUsuarioId(Long usuarioId);

    List<Alerta> findBySectorIdAndUsuarioId(
            Long sectorId,
            Long usuarioId
    );
}
