package cl.duoc.usuarios_service.controller;

import cl.duoc.usuarios_service.repository.CursoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {
    private final CursoRepository repository;
    public CursoController(CursoRepository repository) { this.repository = repository; }
    public record CursoDto(Long id, String codigo, String nombre) {}

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<CursoDto> listar() {
        return repository.findAll().stream()
            .map(c -> new CursoDto(c.getId(), c.getCodigo(), c.getNombre()))
            .sorted(java.util.Comparator.comparing(CursoDto::codigo)).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CursoDto buscar(@PathVariable Long id) {
        var c = repository.findById(id).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Curso inexistente"));
        return new CursoDto(c.getId(), c.getCodigo(), c.getNombre());
    }
}
