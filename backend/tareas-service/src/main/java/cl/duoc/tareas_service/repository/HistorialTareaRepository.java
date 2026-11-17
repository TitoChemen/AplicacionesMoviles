package cl.duoc.tareas_service.repository;
import cl.duoc.tareas_service.entity.HistorialTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialTareaRepository extends JpaRepository<HistorialTarea, Long> {
    List<HistorialTarea> findByTareaIdOrderByFechaAscIdAsc(Long tareaId);
    boolean existsByTareaIdAndEvidenciaUrl(Long tareaId, String evidenciaUrl);
    boolean existsByTareaId(Long tareaId);
}
