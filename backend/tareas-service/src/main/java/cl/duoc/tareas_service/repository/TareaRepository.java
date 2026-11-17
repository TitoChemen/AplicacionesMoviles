package cl.duoc.tareas_service.repository;

import cl.duoc.tareas_service.entity.Tarea;
import cl.duoc.tareas_service.entity.EstadoTarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TareaRepository
        extends JpaRepository<Tarea, Long> {

    List<Tarea> findBySectorId(Long sectorId);
    List<Tarea> findByCursoId(Long cursoId);
    List<Tarea> findByCursoIdAndSectorId(Long cursoId, Long sectorId);
    List<Tarea> findByCursoIdAndEstudianteId(Long cursoId, Long estudianteId);
    List<Tarea> findByCursoIdAndEstado(Long cursoId, EstadoTarea estado);

    List<Tarea> findByEstudianteId(Long estudianteId);

    List<Tarea> findByEstado(EstadoTarea estado);
}
