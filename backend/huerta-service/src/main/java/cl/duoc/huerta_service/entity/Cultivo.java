
package cl.duoc.huerta_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cultivos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cultivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sectorId;

    @Column(nullable = false)
    private String nombreComun;

    @Column(length = 1000)
    private String descripcion;

    private String fechaSiembra;

    private String fechaCosechaEstimada;

    private String frecuenciaRiego;

    private String estadoDesarrollo;
}
