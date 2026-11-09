package cl.duoc.huerta_service.controller;

import cl.duoc.huerta_service.entity.Cultivo;
import cl.duoc.huerta_service.service.CultivoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cultivos")
public class CultivoController {

    private final CultivoService cultivoService;

    public CultivoController(CultivoService cultivoService) {
        this.cultivoService = cultivoService;
    }

    @GetMapping
    public List<Cultivo> listar() {
        return cultivoService.listar();
    }

    @GetMapping("/{id}")
    public Cultivo buscar(@PathVariable Long id) {
        return cultivoService.buscar(id);
    }

    @GetMapping("/sector/{sectorId}")
    public List<Cultivo> porSector(
            @PathVariable Long sectorId) {

        return cultivoService.listarPorSector(sectorId);
    }

    @PostMapping
    public ResponseEntity<Cultivo> crear(
            @RequestBody Cultivo cultivo) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cultivoService.crear(cultivo));
    }

    @PutMapping("/{id}")
    public Cultivo actualizar(
            @PathVariable Long id,
            @RequestBody Cultivo cultivo) {

        return cultivoService.actualizar(id, cultivo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {

        cultivoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
