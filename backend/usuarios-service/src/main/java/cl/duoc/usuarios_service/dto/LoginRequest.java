
package cl.duoc.usuarios_service.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank @Email String correo,
        @NotBlank String contrasena
) {}
