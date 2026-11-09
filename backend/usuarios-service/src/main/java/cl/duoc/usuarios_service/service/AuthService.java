
package cl.duoc.usuarios_service.service;

import cl.duoc.usuarios_service.dto.*;
import cl.duoc.usuarios_service.entity.*;
import cl.duoc.usuarios_service.repository.*;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository repository,
            PasswordEncoder encoder,
            JwtService jwtService) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (repository.existsByCorreo(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(
                encoder.encode(request.contrasena()));
        usuario.setRol(Rol.ESTUDIANTE);
        usuario.setActivo(true);

        return UsuarioResponse.from(repository.save(usuario));
    }

    public AuthResponse login(LoginRequest request) {

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        Usuario usuario = repository.findByCorreo(correo)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Credenciales incorrectas"));

        if (!Boolean.TRUE.equals(usuario.getActivo())
                || !encoder.matches(
                request.contrasena(),
                usuario.getContrasenaHash())) {
            throw new BadCredentialsException(
                    "Credenciales incorrectas");
        }

        String token = jwtService.generarToken(usuario);

        return new AuthResponse(
                token,
                "Bearer",
                UsuarioResponse.from(usuario)
        );
    }
}
