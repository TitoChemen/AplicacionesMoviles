
package cl.duoc.usuarios_service.service;

import cl.duoc.usuarios_service.entity.Usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final JwtEncoder encoder;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    public JwtService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(
                        expirationMinutes, ChronoUnit.MINUTES))
                .subject(usuario.getId().toString())
                .claim("rol", usuario.getRol().name())
                .claim("correo", usuario.getCorreo())
                .build();

        JwsHeader header = JwsHeader.with(
                MacAlgorithm.HS256).build();

        return encoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
