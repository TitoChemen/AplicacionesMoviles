
package cl.duoc.usuarios_service.controller;

import cl.duoc.usuarios_service.dto.UsuarioResponse;
import cl.duoc.usuarios_service.service.UsuarioService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public UsuarioResponse perfil(
            @AuthenticationPrincipal Jwt jwt) {

        Long id = Long.valueOf(jwt.getSubject());
        return service.buscar(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    public record NombreRequest(String nombre) {}

    @PatchMapping("/{id}/nombre")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UsuarioResponse actualizarNombre(
            @PathVariable Long id,
            @RequestBody NombreRequest request) {

        return service.actualizarNombre(id, request.nombre());
    }

    public record EstadoRequest(boolean activo) {}

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UsuarioResponse cambiarEstado(
            @PathVariable Long id,
            @RequestBody EstadoRequest request) {

        return service.cambiarActivo(id, request.activo());
    }
}
