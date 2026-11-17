package cl.duoc.usuarios_service.repository;
import cl.duoc.usuarios_service.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CursoRepository extends JpaRepository<Curso, Long> {}
