package cl.duoc.tareas_service.controller;

import cl.duoc.tareas_service.entity.Alerta;
import cl.duoc.tareas_service.service.AlertaService;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Alerta> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Alerta buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/sector/{id}")
    public List<Alerta> porSector(@PathVariable Long id) {
        return service.porSector(id);
    }

    @PostMapping
    public ResponseEntity<Alerta> crear(
            @RequestBody Alerta alerta) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.crear(alerta));
    }

    @PutMapping("/{id}")
    public Alerta actualizar(
            @PathVariable Long id,
            @RequestBody Alerta alerta) {
        return service.actualizar(id, alerta);
    }

    @PatchMapping("/{id}/estado")
    public Alerta cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> datos) {
        return service.cambiarEstado(
                id, datos.get("estado"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
