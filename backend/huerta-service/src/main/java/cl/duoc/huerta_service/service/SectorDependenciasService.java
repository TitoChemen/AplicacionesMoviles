package cl.duoc.huerta_service.service;

import cl.duoc.huerta_service.exception.ConflictoException;
import cl.duoc.huerta_service.security.CursoActual;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;

/** Evita borrar un sector que tenga tareas o alertas en el otro microservicio.
 * No elimina datos entre microservicios ni requiere acceso directo a otras BD.
 */
@Service
public class SectorDependenciasService {
    private final RestClient client;
    private final CursoActual acceso;

    public SectorDependenciasService(
            @Value("${app.services.gateway-url:http://localhost:8080}") String gateway,
            CursoActual acceso) {
        this.acceso = acceso;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().baseUrl(gateway)
                .requestFactory(factory).build();
    }

    public void exigirSinTareasNiAlertas(Long sectorId) {
        try {
            Object[] tareas = consultar("/api/tareas/sector/{id}", sectorId);
            if (tareas != null && tareas.length > 0) {
                throw new ConflictoException(
                    "El sector tiene tareas asignadas o evidencias. Resuélvelas antes de cambiar su curso o archívalo.");
            }
            Object[] alertas = consultar("/api/alertas/sector/{id}", sectorId);
            if (alertas != null && alertas.length > 0) {
                throw new ConflictoException(
                    "El sector tiene alertas registradas. Resuélvelas antes de cambiar su curso o archívalo.");
            }
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo verificar las tareas/alertas. Revisa tareas-service y el gateway antes de cambiar o eliminar el sector.", ex);
        }
    }

    private Object[] consultar(String uri, Long sectorId) {
        return client.get().uri(uri, sectorId)
                .headers(h -> h.setBearerAuth(acceso.token().getToken().getTokenValue()))
                .retrieve().body(Object[].class);
    }
}
