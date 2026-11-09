package cl.duoc.tareas_service.repository;

import cl.duoc.tareas_service.entity.Tarea;
import cl.duoc.tareas_service.entity.EstadoTarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TareaRepository
        extends JpaRepository<Tarea, Long> {

    List<Tarea> findBySectorId(Long sectorId);

    List<Tarea> findByEstudianteId(Long estudianteId);

    List<Tarea> findByEstado(EstadoTarea estado);
}
