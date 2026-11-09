
package cl.duoc.tareas_service.controller;

import cl.duoc.tareas_service.entity.*;
import cl.duoc.tareas_service.service.TareaService;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tareas")
public class TareaController {

    private final TareaService service;

    public TareaController(TareaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Tarea> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Tarea buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/sector/{id}")
    public List<Tarea> porSector(@PathVariable Long id) {
        return service.porSector(id);
    }

    @GetMapping("/estudiante/{id}")
    public List<Tarea> porEstudiante(@PathVariable Long id) {
        return service.porEstudiante(id);
    }

    @GetMapping("/estado/{estado}")
    public List<Tarea> porEstado(
            @PathVariable EstadoTarea estado) {
        return service.porEstado(estado);
    }

    @PostMapping
    public ResponseEntity<Tarea> crear(
            @RequestBody Tarea tarea) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.crear(tarea));
    }

    @PutMapping("/{id}")
    public Tarea actualizar(
            @PathVariable Long id,
            @RequestBody Tarea tarea) {

        return service.actualizar(id, tarea);
    }

    @PatchMapping("/{id}/iniciar")
    public Tarea iniciar(@PathVariable Long id) {
        return service.iniciar(id);
    }

    // ==========================================
    // EVIDENCIA POR URL
    // ==========================================

    public record EvidenciaRequest(
            String evidenciaUrl) {}

    @PostMapping(
            value = "/{id}/evidencias",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Tarea entregarEvidencia(
            @PathVariable Long id,
            @RequestBody EvidenciaRequest request) {

        return service.entregarEvidencia(
                id,
                request.evidenciaUrl()
        );
    }

    // ==========================================
    // SUBIR FOTOGRAFIA
    // ==========================================

    @PostMapping(
            value = "/{id}/evidencias/archivo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Tarea subirFotografia(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo) {

        return service.entregarEvidenciaArchivo(
                id,
                archivo
        );
    }

    // ==========================================
    // DESCARGAR FOTOGRAFIA
    // ==========================================

    @GetMapping("/{id}/evidencias/archivo")
    public ResponseEntity<Resource> descargarFotografia(
            @PathVariable Long id,
            @RequestParam("nombre") String nombre) {

        Resource recurso =
                service.obtenerEvidenciaArchivo(id, nombre);

        MediaType tipo = nombre.endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store"
                )
                .body(recurso);
    }

    // ==========================================
    // VALIDACION DE EVIDENCIA
    // ==========================================

    public record ValidacionRequest(
            boolean aprobada,
            String observacion) {}

    @PatchMapping("/{id}/validacion")
    public Tarea validar(
            @PathVariable Long id,
            @RequestBody ValidacionRequest request) {

        return service.validarEvidencia(
                id,
                request.aprobada(),
                request.observacion()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {

        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
