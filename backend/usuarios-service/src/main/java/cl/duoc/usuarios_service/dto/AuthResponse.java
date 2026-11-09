
package cl.duoc.usuarios_service.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UsuarioResponse usuario
) {}
