
package cl.duoc.tareas_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class ReferenciaService {

    private final RestClient client;

    public ReferenciaService(
            @Value("${app.services.gateway-url:http://localhost:8080}")
            String gatewayUrl) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.client = RestClient.builder()
                .baseUrl(gatewayUrl)
                .requestFactory(factory)
                .build();
    }

    // ==========================================
    // OBTENER JWT DEL USUARIO AUTENTICADO
    // ==========================================

    private String tokenActual() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AccessDeniedException(
                    "No existe un JWT autenticado"
            );
        }

        return jwtAuth.getToken().getTokenValue();
    }

    // ==========================================
    // CONSULTAR USUARIO
    // ==========================================

    private UsuarioResponse consultarUsuario(Long usuarioId) {

        try {
            return client.get()
                    .uri("/api/usuarios/{id}", usuarioId)
                    .headers(headers ->
                            headers.setBearerAuth(tokenActual()))
                    .retrieve()
                    .body(UsuarioResponse.class);

        } catch (HttpClientErrorException.NotFound e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario no existe",
                    e
            );

        } catch (HttpClientErrorException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo autorizar la consulta del usuario",
                    e
            );

        } catch (HttpServerErrorException | ResourceAccessException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El servicio de usuarios no esta disponible",
                    e
            );

        } catch (RestClientException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar el usuario",
                    e
            );
        }
    }

    // ==========================================
    // VALIDAR SECTOR ACTIVO
    // ==========================================

    public void validarSector(Long sectorId) {

        if (sectorId == null || sectorId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID del sector es invalido"
            );
        }

        SectorResponse sector;

        try {
            sector = client.get()
                    .uri("/api/sectores/{id}", sectorId)
                    .headers(headers ->
                            headers.setBearerAuth(tokenActual()))
                    .retrieve()
                    .body(SectorResponse.class);

        } catch (HttpClientErrorException.NotFound e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El sector no existe",
                    e
            );

        } catch (HttpClientErrorException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Error de autorizacion al consultar el sector",
                    e
            );

        } catch (HttpServerErrorException | ResourceAccessException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El servicio de sectores no esta disponible",
                    e
            );

        } catch (RestClientException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo validar el sector",
                    e
            );
        }

        if (sector == null
                || !sectorId.equals(sector.id())
                || !"ACTIVO".equals(sector.estado())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El sector no existe o no esta activo"
            );
        }
    }

    // ==========================================
    // VALIDAR ESTUDIANTE ACTIVO
    // ==========================================

    public void validarEstudiante(Long estudianteId) {

        if (estudianteId == null || estudianteId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID del estudiante es invalido"
            );
        }

        UsuarioResponse usuario = consultarUsuario(estudianteId);

        if (usuario == null
                || !estudianteId.equals(usuario.id())
                || !Boolean.TRUE.equals(usuario.activo())
                || !"ESTUDIANTE".equals(usuario.rol())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario no es un estudiante activo"
            );
        }
    }

    // ==========================================
    // VALIDAR CUALQUIER USUARIO ACTIVO
    // ==========================================

    public void validarUsuarioActivo(Long usuarioId) {

        if (usuarioId == null || usuarioId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID del usuario es invalido"
            );
        }

        UsuarioResponse usuario = consultarUsuario(usuarioId);

        if (usuario == null
                || !usuarioId.equals(usuario.id())
                || !Boolean.TRUE.equals(usuario.activo())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario no existe o esta inactivo"
            );
        }
    }

    // ==========================================
    // RESPUESTAS DE LOS MICROSERVICIOS
    // ==========================================

    public record SectorResponse(
            Long id,
            String estado
    ) {}

    public record UsuarioResponse(
            Long id,
            String rol,
            Boolean activo
    ) {}
}
