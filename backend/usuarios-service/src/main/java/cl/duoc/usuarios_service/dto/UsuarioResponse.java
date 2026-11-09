
package cl.duoc.usuarios_service.dto;

import cl.duoc.usuarios_service.entity.*;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        Rol rol,
        Boolean activo
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNombre(),
                u.getCorreo(),
                u.getRol(),
                u.getActivo()
        );
    }
}
