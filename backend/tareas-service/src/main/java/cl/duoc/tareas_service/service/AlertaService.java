
package cl.duoc.tareas_service.service;

import cl.duoc.tareas_service.entity.Alerta;
import cl.duoc.tareas_service.exception.RecursoNoEncontradoException;
import cl.duoc.tareas_service.repository.AlertaRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class AlertaService {

    private final AlertaRepository repository;
    private final ReferenciaService referenciaService;

    public AlertaService(
            AlertaRepository repository,
            ReferenciaService referenciaService) {

        this.repository = repository;
        this.referenciaService = referenciaService;
    }

    // ==========================================
    // SEGURIDAD Y AUTENTICACION
    // ==========================================

    private boolean esAdministrador() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        return auth != null
                && auth.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals(
                                "ROLE_ADMINISTRADOR"));
    }

    private Long usuarioActualId() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwt)) {
            throw new AccessDeniedException(
                    "Usuario no autenticado");
        }

        try {
            return Long.valueOf(
                    jwt.getToken().getSubject()
            );
        } catch (NumberFormatException e) {
            throw new AccessDeniedException(
                    "Identificador de usuario invalido");
        }
    }

    private void exigirAdministrador() {

        if (!esAdministrador()) {
            throw new AccessDeniedException(
                    "Solo administradores pueden realizar esta accion");
        }
    }

    private void verificarPropietario(Alerta alerta) {

        if (esAdministrador()) {
            return;
        }

        if (alerta.getUsuarioId() == null
                || !alerta.getUsuarioId()
                .equals(usuarioActualId())) {

            throw new AccessDeniedException(
                    "No tienes permiso para acceder a esta alerta");
        }
    }

    // ==========================================
    // CONSULTAS
    // ==========================================

    @Transactional(readOnly = true)
    public List<Alerta> listar() {

        if (esAdministrador()) {
            return repository.findAll();
        }

        return repository.findByUsuarioId(
                usuarioActualId()
        );
    }

    @Transactional(readOnly = true)
    public Alerta buscar(Long id) {

        Alerta alerta = repository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Alerta no encontrada: " + id));

        verificarPropietario(alerta);

        return alerta;
    }

    @Transactional(readOnly = true)
    public List<Alerta> porSector(Long sectorId) {

        if (esAdministrador()) {
            return repository.findBySectorId(sectorId);
        }

        return repository.findBySectorIdAndUsuarioId(
                sectorId,
                usuarioActualId()
        );
    }

    // ==========================================
    // CREAR ALERTA
    // ==========================================

    @Transactional
    public Alerta crear(Alerta alerta) {

        validar(alerta);

        // Comprobar que el sector existe y esta activo.
        referenciaService.validarSector(
                alerta.getSectorId()
        );

        // El ID se genera desde la base de datos.
        alerta.setId(null);

        if (!esAdministrador()) {

            // El estudiante no puede suplantar
            // la identidad de otro usuario.
            alerta.setUsuarioId(usuarioActualId());

        } else {

            // Si el administrador no especifica usuario,
            // la alerta queda asignada a el mismo.
            if (alerta.getUsuarioId() == null) {
                alerta.setUsuarioId(usuarioActualId());
            }

            // Comprobar que el usuario asignado
            // existe y esta activo.
            referenciaService.validarUsuarioActivo(
                    alerta.getUsuarioId()
            );
        }

        // Estado y fecha controlados por el backend.
        alerta.setEstado("PENDIENTE");

        alerta.setFechaReporte(
                LocalDate.now().toString()
        );

        return repository.save(alerta);
    }

    // ==========================================
    // ACTUALIZAR ALERTA
    // ==========================================

    @Transactional
    public Alerta actualizar(Long id, Alerta datos) {

        exigirAdministrador();

        Alerta actual = buscar(id);

        validar(datos);

        // Validar sector antes de modificar datos.
        referenciaService.validarSector(
                datos.getSectorId()
        );

        // Si se proporciona un nuevo usuario,
        // comprobar que exista y este activo.
        if (datos.getUsuarioId() != null) {

            referenciaService.validarUsuarioActivo(
                    datos.getUsuarioId()
            );

            actual.setUsuarioId(datos.getUsuarioId());
        }

        actual.setSectorId(datos.getSectorId());
        actual.setTipo(datos.getTipo());
        actual.setDescripcion(datos.getDescripcion());

        // Conservamos estado y fecha originales.
        return repository.save(actual);
    }

    // ==========================================
    // CAMBIAR ESTADO
    // ==========================================

    @Transactional
    public Alerta cambiarEstado(
            Long id,
            String estado) {

        exigirAdministrador();

        Alerta alerta = buscar(id);

        if (estado == null
                || (!estado.equals("PENDIENTE")
                && !estado.equals("EN_REVISION")
                && !estado.equals("RESUELTA"))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado de alerta invalido"
            );
        }

        alerta.setEstado(estado);

        return repository.save(alerta);
    }

    // ==========================================
    // ELIMINAR ALERTA
    // ==========================================

    @Transactional
    public void eliminar(Long id) {

        exigirAdministrador();

        repository.delete(buscar(id));
    }

    // ==========================================
    // VALIDACIONES LOCALES
    // ==========================================

    private void validar(Alerta alerta) {

        if (alerta == null
                || alerta.getSectorId() == null
                || alerta.getSectorId() <= 0
                || alerta.getTipo() == null
                || alerta.getTipo().isBlank()
                || alerta.getDescripcion() == null
                || alerta.getDescripcion().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sector, tipo y descripcion son obligatorios"
            );
        }
    }
}
