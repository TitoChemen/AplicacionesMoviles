
package cl.duoc.tareas_service.service;

import cl.duoc.tareas_service.entity.*;
import cl.duoc.tareas_service.exception.*;
import cl.duoc.tareas_service.repository.*;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TareaService {

    private final TareaRepository repository;
    private final ReferenciaService referenciaService;
    private final ArchivoService archivoService;

    public TareaService(
            TareaRepository repository,
            ReferenciaService referenciaService,
            ArchivoService archivoService) {

        this.repository = repository;
        this.referenciaService = referenciaService;
        this.archivoService = archivoService;
    }

    // ==========================================
    // SEGURIDAD Y AUTENTICACION
    // ==========================================

    private boolean esAdministrador() {

        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();

        return auth != null
                && auth.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals(
                                "ROLE_ADMINISTRADOR"));
    }

    private Long usuarioActualId() {

        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AccessDeniedException(
                    "No existe un JWT autenticado");
        }

        try {
            return Long.valueOf(
                    jwtAuth.getToken().getSubject());

        } catch (NumberFormatException ex) {
            throw new AccessDeniedException(
                    "El JWT no contiene un ID valido");
        }
    }

    private void verificarPropietario(Tarea tarea) {

        if (esAdministrador()) {
            return;
        }

        Long usuarioId = usuarioActualId();

        if (tarea.getEstudianteId() == null
                || !tarea.getEstudianteId().equals(usuarioId)) {

            throw new AccessDeniedException(
                    "No tienes permiso para acceder a esta tarea");
        }
    }

    private void exigirAdministrador() {

        if (!esAdministrador()) {
            throw new AccessDeniedException(
                    "Solo un administrador puede realizar esta operacion");
        }
    }

    // ==========================================
    // CONSULTAS
    // ==========================================

    @Transactional(readOnly = true)
    public List<Tarea> listar() {

        if (esAdministrador()) {
            return repository.findAll();
        }

        return repository.findByEstudianteId(
                usuarioActualId());
    }

    @Transactional(readOnly = true)
    public Tarea buscar(Long id) {

        Tarea tarea = repository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Tarea no encontrada: " + id));

        verificarPropietario(tarea);

        return tarea;
    }

    @Transactional(readOnly = true)
    public List<Tarea> porSector(Long sectorId) {

        if (esAdministrador()) {
            return repository.findBySectorId(sectorId);
        }

        Long usuarioId = usuarioActualId();

        return repository.findBySectorId(sectorId)
                .stream()
                .filter(t ->
                        usuarioId.equals(t.getEstudianteId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Tarea> porEstudiante(Long estudianteId) {

        if (!esAdministrador()
                && !usuarioActualId().equals(estudianteId)) {

            throw new AccessDeniedException(
                    "No puedes consultar tareas de otro estudiante");
        }

        return repository.findByEstudianteId(estudianteId);
    }

    @Transactional(readOnly = true)
    public List<Tarea> porEstado(EstadoTarea estado) {

        if (esAdministrador()) {
            return repository.findByEstado(estado);
        }

        Long usuarioId = usuarioActualId();

        return repository.findByEstado(estado)
                .stream()
                .filter(t ->
                        usuarioId.equals(t.getEstudianteId()))
                .toList();
    }

    // ==========================================
    // CREACION DE TAREAS
    // ==========================================

    @Transactional
    public Tarea crear(Tarea tarea) {

        exigirAdministrador();
        validar(tarea);

        referenciaService.validarSector(
                tarea.getSectorId());

        referenciaService.validarEstudiante(
                tarea.getEstudianteId());

        tarea.setId(null);
        tarea.setEstado(EstadoTarea.PENDIENTE);
        tarea.setEvidenciaUrl(null);
        tarea.setObservacionDocente(null);

        return repository.save(tarea);
    }

    // ==========================================
    // ACTUALIZACION DE TAREAS
    // ==========================================

    @Transactional
    public Tarea actualizar(Long id, Tarea datos) {

        exigirAdministrador();

        Tarea actual = buscar(id);
        validar(datos);

        referenciaService.validarSector(
                datos.getSectorId());

        referenciaService.validarEstudiante(
                datos.getEstudianteId());

        actual.setSectorId(datos.getSectorId());
        actual.setTitulo(datos.getTitulo());
        actual.setDescripcion(datos.getDescripcion());
        actual.setEstudianteId(datos.getEstudianteId());
        actual.setFechaAsignacion(datos.getFechaAsignacion());
        actual.setFechaVencimiento(datos.getFechaVencimiento());

        return repository.save(actual);
    }

    // ==========================================
    // INICIAR TAREA
    // ==========================================

    @Transactional
    public Tarea iniciar(Long id) {

        Tarea tarea = buscar(id);

        if (tarea.getEstado() != EstadoTarea.PENDIENTE) {
            throw new IllegalArgumentException(
                    "La tarea no esta pendiente");
        }

        tarea.setEstado(EstadoTarea.EN_PROGRESO);

        return repository.save(tarea);
    }

    // ==========================================
    // VALIDAR ESTADO PARA EVIDENCIA
    // ==========================================

    private void comprobarEntregaPermitida(Tarea tarea) {

        if (tarea.getEstado() != EstadoTarea.PENDIENTE
                && tarea.getEstado() != EstadoTarea.EN_PROGRESO
                && tarea.getEstado() != EstadoTarea.RECHAZADA) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La tarea no admite evidencia"
            );
        }
    }

    // ==========================================
    // ENTREGA DE EVIDENCIA POR URL
    // ==========================================

    @Transactional
    public Tarea entregarEvidencia(Long id, String url) {

        Tarea tarea = buscar(id);

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "La evidencia es obligatoria");
        }

        comprobarEntregaPermitida(tarea);

        tarea.setEvidenciaUrl(url);
        tarea.setObservacionDocente(null);
        tarea.setEstado(EstadoTarea.PENDIENTE_VALIDACION);

        return repository.save(tarea);
    }

    // ==========================================
    // ENTREGA DE EVIDENCIA FOTOGRAFICA
    // ==========================================

    @Transactional
    public Tarea entregarEvidenciaArchivo(
            Long id,
            MultipartFile archivo) {

        Tarea tarea = buscar(id);

        // Primero comprobar permisos y estado.
        comprobarEntregaPermitida(tarea);

        // Guardar imagen validada.
        String nombre = archivoService.guardarImagen(archivo);

        try {
            tarea.setEvidenciaUrl(
                    "/api/tareas/" + id
                            + "/evidencias/archivo"
            );

            tarea.setObservacionDocente(null);
            tarea.setEstado(
                    EstadoTarea.PENDIENTE_VALIDACION
            );

            Tarea guardada = repository.saveAndFlush(tarea);

            // La ruta del archivo se vincula a la tarea
            // mediante el nombre persistido abajo.
            guardada.setEvidenciaUrl(
                    "/api/tareas/" + id
                            + "/evidencias/archivo?nombre="
                            + nombre
            );

            return repository.saveAndFlush(guardada);

        } catch (RuntimeException e) {
            archivoService.eliminar(nombre);
            throw e;
        }
    }

    // ==========================================
    // CONSULTAR ARCHIVO DE EVIDENCIA
    // ==========================================

    @Transactional(readOnly = true)
    public Resource obtenerEvidenciaArchivo(
            Long id,
            String nombre) {

        Tarea tarea = buscar(id);

        String rutaEsperada =
                "/api/tareas/" + id
                        + "/evidencias/archivo?nombre="
                        + nombre;

        if (!rutaEsperada.equals(tarea.getEvidenciaUrl())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La evidencia no corresponde a esta tarea"
            );
        }

        return archivoService.obtenerImagen(nombre);
    }

    // ==========================================
    // VALIDACION DOCENTE
    // ==========================================

    @Transactional
    public Tarea validarEvidencia(
            Long id,
            boolean aprobada,
            String observacion) {

        exigirAdministrador();

        Tarea tarea = buscar(id);

        if (tarea.getEstado()
                != EstadoTarea.PENDIENTE_VALIDACION) {

            throw new IllegalArgumentException(
                    "La tarea no esta pendiente de validacion");
        }

        tarea.setEstado(
                aprobada
                        ? EstadoTarea.APROBADA
                        : EstadoTarea.RECHAZADA
        );

        tarea.setObservacionDocente(observacion);

        return repository.save(tarea);
    }

    // ==========================================
    // ELIMINAR TAREA
    // ==========================================

    @Transactional
    public void eliminar(Long id) {

        exigirAdministrador();

        Tarea tarea = buscar(id);

        repository.delete(tarea);
    }

    // ==========================================
    // VALIDACIONES LOCALES
    // ==========================================

    private void validar(Tarea tarea) {

        if (tarea == null) {
            throw new IllegalArgumentException(
                    "Los datos de la tarea son obligatorios");
        }

        if (tarea.getSectorId() == null
                || tarea.getSectorId() <= 0) {

            throw new IllegalArgumentException(
                    "El sector debe ser un ID valido");
        }

        if (tarea.getEstudianteId() == null
                || tarea.getEstudianteId() <= 0) {

            throw new IllegalArgumentException(
                    "El estudiante debe ser un ID valido");
        }

        if (tarea.getTitulo() == null
                || tarea.getTitulo().isBlank()) {

            throw new IllegalArgumentException(
                    "El titulo es obligatorio");
        }

        if (tarea.getFechaAsignacion() == null
                || tarea.getFechaAsignacion().isBlank()
                || tarea.getFechaVencimiento() == null
                || tarea.getFechaVencimiento().isBlank()) {

            throw new IllegalArgumentException(
                    "Las fechas son obligatorias");
        }
    }
}
