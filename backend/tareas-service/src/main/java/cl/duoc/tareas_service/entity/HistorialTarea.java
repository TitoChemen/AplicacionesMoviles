package cl.duoc.tareas_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarea_historial")
@Getter @Setter
public class HistorialTarea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tarea_id", nullable = false)
    private Long tareaId;
    @Column(nullable = false)
    private Integer intento;
    @Column(nullable = false, length = 32)
    private String accion;
    @Column(name = "actor_id", nullable = false)
    private Long actorId;
    @Column(length = 1000)
    private String comentario;
    @Column(name = "evidencia_url", length = 1000)
    private String evidenciaUrl;
    @Column(nullable = false)
    private LocalDateTime fecha;
}
