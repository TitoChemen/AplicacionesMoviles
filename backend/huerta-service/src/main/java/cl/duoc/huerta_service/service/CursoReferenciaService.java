package cl.duoc.huerta_service.service;

import cl.duoc.huerta_service.security.CursoActual;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;

@Service
public class CursoReferenciaService {
    private final RestClient client;
    private final CursoActual acceso;
    public CursoReferenciaService(
        @Value("${app.services.gateway-url:http://localhost:8080}") String gateway,
        CursoActual acceso) {
        this.acceso = acceso;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().baseUrl(gateway).requestFactory(factory).build();
    }
    public record CursoDto(Long id, String codigo, String nombre) {}
    public void exigirCursoExistente(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un curso valido");
        }
        try {
            CursoDto curso = client.get().uri("/api/cursos/{id}", id)
                .headers(h -> h.setBearerAuth(acceso.token().getToken().getTokenValue()))
                .retrieve().body(CursoDto.class);
            if (curso == null || !id.equals(curso.id()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Curso no valido");
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso no existe", e);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado para consultar cursos", e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "No se pudo verificar el curso; revisa usuarios-service y el gateway", e);
        }
    }
}
