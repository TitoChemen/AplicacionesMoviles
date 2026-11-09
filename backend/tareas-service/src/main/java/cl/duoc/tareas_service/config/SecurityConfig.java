
package cl.duoc.tareas_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // ==========================================
    // VALIDACION DE JWT
    // ==========================================

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer) {

        byte[] key = Base64.getDecoder().decode(secret);

        if (key.length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET debe tener al menos 32 bytes"
            );
        }

        var secretKey = new SecretKeySpec(
                key,
                "HmacSHA256"
        );

        var decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(issuer)
        );

        return decoder;
    }

    // ==========================================
    // CONVERSION DE ROLES
    // ==========================================

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            String rol = jwt.getClaimAsString("rol");

            if (rol == null) {
                return List.of();
            }

            return List.of(
                    new SimpleGrantedAuthority("ROLE_" + rol)
            );
        });

        return converter;
    }

    // ==========================================
    // SEGURIDAD DE ENDPOINTS
    // ==========================================

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter converter)
            throws Exception {

        return http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // ==================================
                        // ALERTAS
                        // ==================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/alertas",
                                "/api/alertas/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/alertas"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/alertas/**"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/alertas/**"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/alertas/**"
                        ).hasRole("ADMINISTRADOR")

                        // ==================================
                        // TAREAS - CONSULTAS
                        // ==================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/tareas",
                                "/api/tareas/**"
                        ).authenticated()

                        // ==================================
                        // TAREAS - EVIDENCIAS
                        // ==================================

                        // Entrega de evidencia mediante URL
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/tareas/*/evidencias"
                        ).authenticated()

                        // Nueva entrega de fotografia real
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/tareas/*/evidencias/archivo"
                        ).authenticated()

                        // ==================================
                        // TAREAS - ESTUDIANTE
                        // ==================================

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/tareas/*/iniciar"
                        ).authenticated()

                        // ==================================
                        // TAREAS - ADMINISTRADOR
                        // ==================================

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/tareas/*/validacion"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/tareas"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/tareas/**"
                        ).hasRole("ADMINISTRADOR")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/tareas/**"
                        ).hasRole("ADMINISTRADOR")

                        // ==================================
                        // RESTO DE PETICIONES
                        // ==================================

                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        converter
                                )
                        )
                )

                .build();
    }
}
