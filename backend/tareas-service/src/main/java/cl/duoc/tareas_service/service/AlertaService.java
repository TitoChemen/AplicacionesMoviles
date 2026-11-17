
package cl.duoc.tareas_service.service;

import cl.duoc.tareas_service.entity.Alerta;
import cl.duoc.tareas_service.exception.RecursoNoEncontradoException;
import cl.duoc.tareas_service.repository.AlertaRepository;
import cl.duoc.tareas_service.security.CursoActual;

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
    private final CursoActual acceso;

    public AlertaService(
            AlertaRepository repository,
            ReferenciaService referenciaService, CursoActual acceso) {

        this.repository = repository;
        this.referenciaService = referenciaService;
        this.acceso = acceso;
    }

    private boolean esAdministrador() { return acceso.esDocente(); }
    private Long usuarioActualId() { return acceso.usuarioId(); }
    private void exigirAdministrador() { acceso.exigirDocente(); }
    private void verificarPropietario(Alerta alerta) {
        acceso.exigirCurso(alerta.getCursoId());
        if (!esAdministrador() &&
                (alerta.getUsuarioId() == null ||
                 !alerta.getUsuarioId().equals(usuarioActualId()))) {
            throw new AccessDeniedException("No tienes acceso a esta alerta");
        }
    }

    // ==========================================
    // CONSULTAS
    // ==========================================

    @Transactional(readOnly = true)
    public List<Alerta> listar() {

        if (acceso.esAdminGlobal()) return repository.findAll();
        if (esAdministrador()) {
            return repository.findByCursoId(acceso.id());
        }

        return repository.findByCursoIdAndUsuarioId(
                acceso.id(), usuarioActualId());
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

        if (acceso.esAdminGlobal()) return repository.findBySectorId(sectorId);
        if (esAdministrador()) {
            return repository.findByCursoIdAndSectorId(acceso.id(), sectorId);
        }

        return repository.findByCursoIdAndSectorIdAndUsuarioId(
                acceso.id(), sectorId, usuarioActualId());
    }

    // ==========================================
    // CREAR ALERTA
    // ==========================================

    @Transactional
    public Alerta crear(Alerta alerta) {

        validar(alerta);

        // Comprobar que el sector existe y esta activo.
        Long cursoSector = referenciaService.validarSector(alerta.getSectorId());

        // El ID se genera desde la base de datos.
        alerta.setId(null);
        alerta.setCursoId(cursoSector);

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
            Long cursoUsuario = referenciaService.validarUsuarioActivo(alerta.getUsuarioId());
            if (!cursoSector.equals(cursoUsuario)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El usuario y el sector deben pertenecer al mismo curso");
            }
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
        Long nuevoCurso = referenciaService.validarSector(datos.getSectorId());

        // Si se proporciona un nuevo usuario,
        // comprobar que exista y este activo.
        if (datos.getUsuarioId() != null) {

            Long cursoUsuario = referenciaService.validarUsuarioActivo(datos.getUsuarioId());
            if (!nuevoCurso.equals(cursoUsuario)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El usuario y el sector deben pertenecer al mismo curso");
            }
            actual.setUsuarioId(datos.getUsuarioId());
        }

        if (actual.getUsuarioId() != null && datos.getUsuarioId() == null) {
            Long cursoUsuario = referenciaService.validarUsuarioActivo(actual.getUsuarioId());
            if (!nuevoCurso.equals(cursoUsuario)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El usuario actual no pertenece al nuevo sector");
            }
        }
        actual.setSectorId(datos.getSectorId());
        actual.setCursoId(nuevoCurso); // deriva del sector validado, no del body.
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
