package cl.duoc.huerta_service.controller;

import cl.duoc.huerta_service.entity.Sector;
import cl.duoc.huerta_service.service.SectorService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sectores")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping
    public List<Sector> listar() {
        return sectorService.listar();
    }

    @GetMapping("/{id}")
    public Sector buscar(@PathVariable Long id) {
        return sectorService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Sector> crear(
            @RequestBody Sector sector) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sectorService.crear(sector));
    }

    @PutMapping("/{id}")
    public Sector actualizar(
            @PathVariable Long id,
            @RequestBody Sector sector) {

        return sectorService.actualizar(id, sector);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {

        sectorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
