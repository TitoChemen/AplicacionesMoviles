package cl.duoc.usuarios_service.service;

import cl.duoc.usuarios_service.dto.UsuarioResponse;
import cl.duoc.usuarios_service.entity.Usuario;
import cl.duoc.usuarios_service.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    private JwtAuthenticationToken token() {
        if (!(SecurityContextHolder.getContext().getAuthentication()
                instanceof JwtAuthenticationToken jwt)) {
            throw new AccessDeniedException("Autenticacion requerida");
        }
        return jwt;
    }

    private boolean esAdminGlobal() {
        return "ADMINISTRADOR".equals(token().getToken().getClaimAsString("rol"))
                && Boolean.TRUE.equals(token().getToken().getClaim("adminGlobal"));
    }

    private Long cursoActual() {
        String id = token().getToken().getClaimAsString("cursoId");
        if (id == null || id.isBlank()) {
            throw new AccessDeniedException("Cuenta sin curso asignado");
        }
        try { return Long.valueOf(id); }
        catch (NumberFormatException e) {
            throw new AccessDeniedException("Curso JWT invalido");
        }
    }

    private Usuario buscarEntidad(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Usuario buscarEntidadDelCurso(Long id) {
        Usuario u = buscarEntidad(id);
        if (!esAdminGlobal() && !cursoActual().equals(u.getCursoId())) {
            throw new AccessDeniedException("El usuario pertenece a otro curso");
        }
        return u;
    }

    // /me devuelve el perfil asociado al JWT, sin aceptar otro ID.
    public UsuarioResponse miPerfil(Long id) {
        if (!id.toString().equals(token().getToken().getSubject())) {
            throw new AccessDeniedException("Perfil ajeno");
        }
        return UsuarioResponse.from(buscarEntidad(id));
    }

    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.from(buscarEntidadDelCurso(id));
    }

    public List<UsuarioResponse> listar() {
        return (esAdminGlobal() ? repository.findAll() : repository.findByCursoId(cursoActual()))
                .stream().map(UsuarioResponse::from).toList();
    }

    @Transactional
    public UsuarioResponse actualizarNombre(Long id, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        Usuario u = buscarEntidadDelCurso(id);
        u.setNombre(nombre.trim());
        return UsuarioResponse.from(repository.save(u));
    }

    @Transactional
    public UsuarioResponse cambiarActivo(Long id, boolean activo) {
        Usuario u = buscarEntidadDelCurso(id);
        // Evitar bloquear cuentas docentes desde el panel de un curso.
        if (u.getRol() != cl.duoc.usuarios_service.entity.Rol.ESTUDIANTE) {
            throw new AccessDeniedException("No puedes cambiar el estado de una cuenta docente o administradora");
        }
        u.setActivo(activo);
        return UsuarioResponse.from(repository.save(u));
    }

    @Transactional
    public UsuarioResponse asignarCurso(Long id) {
        Usuario u = buscarEntidad(id);
        if (u.getRol() != cl.duoc.usuarios_service.entity.Rol.ESTUDIANTE) {
            throw new AccessDeniedException("Solo los estudiantes pueden ser matriculados");
        }
        // Solo permite matricular alumnos que aun no tienen curso o que ya son del mismo.
        Long curso = cursoActual();
        if (u.getCursoId() != null && !curso.equals(u.getCursoId())) {
            throw new AccessDeniedException("Alumno ya matriculado en otro curso");
        }
        u.setCursoId(curso);
        return UsuarioResponse.from(repository.saveAndFlush(u));
    }
}
