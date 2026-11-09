
package cl.duoc.usuarios_service.service;

import cl.duoc.usuarios_service.dto.UsuarioResponse;
import cl.duoc.usuarios_service.entity.Usuario;
import cl.duoc.usuarios_service.repository.UsuarioRepository;

import org.springframework.http.HttpStatus;
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

    public Usuario buscarEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"));
    }

    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.from(buscarEntidad(id));
    }

    public List<UsuarioResponse> listar() {
        return repository.findAll()
                .stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Transactional
    public UsuarioResponse actualizarNombre(
            Long id, String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre es obligatorio");
        }

        Usuario usuario = buscarEntidad(id);
        usuario.setNombre(nombre.trim());

        return UsuarioResponse.from(repository.save(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarActivo(
            Long id, boolean activo) {

        Usuario usuario = buscarEntidad(id);
        usuario.setActivo(activo);

        return UsuarioResponse.from(repository.save(usuario));
    }
}
