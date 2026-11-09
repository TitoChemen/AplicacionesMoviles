package cl.duoc.tareas_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alertas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sectorId;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    private Long usuarioId;

    private String fechaReporte;

    @Column(nullable = false)
    private String estado = "PENDIENTE";
}
