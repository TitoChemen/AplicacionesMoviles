package cl.duoc.huerta_service.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CursoActual {
    public JwtAuthenticationToken token() {
        if (!(SecurityContextHolder.getContext().getAuthentication()
                instanceof JwtAuthenticationToken jwt)) {
            throw new AccessDeniedException("No hay autenticacion JWT");
        }
        return jwt;
    }
    public Long usuarioId() {
        try { return Long.valueOf(token().getToken().getSubject()); }
        catch (NumberFormatException e) {
            throw new AccessDeniedException("ID de usuario invalido");
        }
    }
    public Long id() {
        String id = token().getToken().getClaimAsString("cursoId");
        if (id == null || id.isBlank()) {
            throw new AccessDeniedException("Cuenta sin curso asignado");
        }
        try { return Long.valueOf(id); }
        catch (NumberFormatException e) {
            throw new AccessDeniedException("Curso JWT invalido");
        }
    }
    public boolean esAdminGlobal() {
        return Boolean.TRUE.equals(token().getToken().getClaim("adminGlobal"))
                && token().getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
    }
    public boolean esDocente() {
        return esAdminGlobal() || token().getAuthorities().stream()
                .anyMatch(a -> "ROLE_DOCENTE".equals(a.getAuthority()));
    }
    public void exigirDocente() {
        if (!esDocente()) throw new AccessDeniedException("Solo personal docente o administrador");
    }
    public void exigirAdminGlobal() {
        if (!esAdminGlobal()) throw new AccessDeniedException("Solo el administrador global puede gestionar sectores");
    }
    public void exigirCurso(Long id) {
        if (esAdminGlobal()) return; // el administrador puede consultar recursos de todos los cursos
        if (id == null || !id().equals(id)) {
            throw new AccessDeniedException("Recurso de otro curso");
        }
    }
}
