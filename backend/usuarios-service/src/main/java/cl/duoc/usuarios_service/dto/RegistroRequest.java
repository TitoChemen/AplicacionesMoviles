
package cl.duoc.usuarios_service.dto;

import jakarta.validation.constraints.*;

public record RegistroRequest(
        @NotBlank String nombre,
        @NotBlank @Email String correo,
        @NotBlank @Size(min = 8) String contrasena
) {}
