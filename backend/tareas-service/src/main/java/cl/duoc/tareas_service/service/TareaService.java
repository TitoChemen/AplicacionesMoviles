
package cl.duoc.tareas_service.service;

import cl.duoc.tareas_service.entity.*;
import cl.duoc.tareas_service.exception.*;
import cl.duoc.tareas_service.repository.*;
import cl.duoc.tareas_service.security.CursoActual;

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
    private final CursoActual acceso;
    private final HistorialTareaRepository historialRepository;

    public TareaService(
            TareaRepository repository,
            ReferenciaService referenciaService,
            ArchivoService archivoService, CursoActual acceso, HistorialTareaRepository historialRepository) {

        this.repository = repository;
        this.referenciaService = referenciaService;
        this.archivoService = archivoService;
        this.acceso = acceso;
        this.historialRepository = historialRepository;
    }

    // Reglas de acceso: ni el docente ni el alumno pueden salir de su curso.
    private boolean esAdministrador() { return acceso.esDocente(); }
    private Long usuarioActualId() { return acceso.usuarioId(); }
    private void exigirAdministrador() { acceso.exigirDocente(); }

    private void verificarPropietario(Tarea tarea) {
        acceso.exigirCurso(tarea.getCursoId());
        if (!esAdministrador() &&
                (tarea.getEstudianteId() == null ||
                 !tarea.getEstudianteId().equals(usuarioActualId()))) {
            throw new AccessDeniedException("No tienes acceso a esta tarea");
        }
    }

    private void exigirEstudianteAsignado(Tarea tarea) {
        if (esAdministrador() || tarea.getEstudianteId() == null ||
                !tarea.getEstudianteId().equals(usuarioActualId())) {
            throw new AccessDeniedException("Solo el estudiante asignado puede entregar evidencia");
        }
    }

    // ==========================================
    // CONSULTAS
    // ==========================================

    @Transactional(readOnly = true)
    public List<Tarea> listar() {

        if (acceso.esAdminGlobal()) return repository.findAll();
        if (esAdministrador()) {
            return repository.findByCursoId(acceso.id());
        }

        return repository.findByCursoIdAndEstudianteId(
                acceso.id(), usuarioActualId());
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

        if (acceso.esAdminGlobal()) return repository.findBySectorId(sectorId);
        if (esAdministrador()) {
            return repository.findByCursoIdAndSectorId(acceso.id(), sectorId);
        }

        Long usuarioId = usuarioActualId();

        return repository.findByCursoIdAndSectorId(acceso.id(), sectorId)
                .stream().filter(t -> usuarioId.equals(t.getEstudianteId())).toList();
    }

    @Transactional(readOnly = true)
    public List<Tarea> porEstudiante(Long estudianteId) {

        if (!esAdministrador()
                && !usuarioActualId().equals(estudianteId)) {

            throw new AccessDeniedException(
                    "No puedes consultar tareas de otro estudiante");
        }

        if (acceso.esAdminGlobal()) return repository.findByEstudianteId(estudianteId);
        return repository.findByCursoIdAndEstudianteId(acceso.id(), estudianteId);
    }

    @Transactional(readOnly = true)
    public List<Tarea> porEstado(EstadoTarea estado) {

        if (acceso.esAdminGlobal()) return repository.findByEstado(estado);
        if (esAdministrador()) {
            return repository.findByCursoIdAndEstado(acceso.id(), estado);
        }

        Long usuarioId = usuarioActualId();

        return repository.findByCursoIdAndEstado(acceso.id(), estado)
                .stream().filter(t -> usuarioId.equals(t.getEstudianteId())).toList();
    }

    // ==========================================
    // CREACION DE TAREAS
    // ==========================================

    @Transactional
    public Tarea crear(Tarea tarea) {

        exigirAdministrador();
        validar(tarea);

        Long cursoSector = referenciaService.validarSector(tarea.getSectorId());
        Long cursoAlumno = referenciaService.validarEstudiante(tarea.getEstudianteId());
        if (!cursoSector.equals(cursoAlumno)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estudiante y el sector deben pertenecer al mismo curso");
        }
        tarea.setId(null);
        // Curso obtenido de las referencias validadas, NUNCA del body.
        tarea.setCursoId(cursoSector);
        tarea.setEstado(EstadoTarea.PENDIENTE);
        tarea.setEvidenciaUrl(null);
        tarea.setObservacionDocente(null);
        tarea.setComentarioEstudiante(null);
        tarea.setIntentoActual(0);

        return repository.save(tarea);
    }

    // ==========================================
    // ACTUALIZACION DE TAREAS
    // ==========================================

    @Transactional
    public Tarea actualizar(Long id, Tarea datos) {

        exigirAdministrador();

        Tarea actual = buscar(id);
        if (historialRepository.existsByTareaId(id) ||
                (actual.getEvidenciaUrl() != null && !actual.getEvidenciaUrl().isBlank())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta tarea ya tiene evidencia o historial; crea otra en vez de modificarla");
        }
        validar(datos);

        Long cursoSector = referenciaService.validarSector(datos.getSectorId());
        Long cursoAlumno = referenciaService.validarEstudiante(datos.getEstudianteId());
        if (!cursoSector.equals(cursoAlumno)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estudiante y el sector deben pertenecer al mismo curso");
        }
        actual.setCursoId(cursoSector);
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
        exigirEstudianteAsignado(tarea);

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
    public Tarea entregarEvidencia(Long id, String url, String justificacion) {

        Tarea tarea = buscar(id);
        exigirEstudianteAsignado(tarea);

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "La evidencia es obligatoria");
        }

        comprobarEntregaPermitida(tarea);

        tarea.setEvidenciaUrl(url);
        tarea.setComentarioEstudiante(normalizarJustificacion(justificacion));
        tarea.setObservacionDocente(null);
        tarea.setEstado(EstadoTarea.PENDIENTE_VALIDACION);
        tarea.setIntentoActual(tarea.getIntentoActual() + 1);
        Tarea guardada = repository.save(tarea);
        registrarEvento(guardada, "ENTREGA", guardada.getComentarioEstudiante());
        return guardada;
    }

    // ==========================================
    // ENTREGA DE EVIDENCIA FOTOGRAFICA
    // ==========================================

    @Transactional
    public Tarea entregarEvidenciaArchivo(
            Long id,
            MultipartFile archivo,
            String justificacion) {

        Tarea tarea = buscar(id);
        exigirEstudianteAsignado(tarea);

        // Primero comprobar permisos y estado.
        comprobarEntregaPermitida(tarea);

        // Guardar imagen validada.
        String nombre = archivoService.guardarImagen(archivo);

        try {
            tarea.setEvidenciaUrl(
                    "/api/tareas/" + id
                            + "/evidencias/archivo"
            );

            tarea.setComentarioEstudiante(normalizarJustificacion(justificacion));
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

            guardada.setIntentoActual(guardada.getIntentoActual() + 1);
            Tarea finalizada = repository.saveAndFlush(guardada);
            registrarEvento(finalizada, "ENTREGA", finalizada.getComentarioEstudiante());
            return finalizada;

        } catch (RuntimeException e) {
            archivoService.eliminar(nombre);
            throw e;
        }
    }

    private String normalizarJustificacion(String texto) {
        String valor = texto == null ? "" : texto.trim();
        if (valor.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La justificacion no puede superar los 1000 caracteres");
        }
        return valor.isEmpty() ? null : valor;
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

        if (!rutaEsperada.equals(tarea.getEvidenciaUrl()) &&
                !historialRepository.existsByTareaIdAndEvidenciaUrl(id, rutaEsperada)) {
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

        String comentario = normalizarJustificacion(observacion);
        if (!aprobada && comentario == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debes explicar el motivo del rechazo");
        }
        tarea.setObservacionDocente(comentario);
        Tarea guardada = repository.save(tarea);
        registrarEvento(guardada, aprobada ? "APROBACION" : "RECHAZO", comentario);
        return guardada;
    }

    // Historial de la tarea autorizado por buscar(id): propietario o personal de su curso.
    @Transactional(readOnly = true)
    public List<HistorialTarea> historial(Long id) {
        buscar(id);
        return historialRepository.findByTareaIdOrderByFechaAscIdAsc(id);
    }

    private void registrarEvento(Tarea tarea, String accion, String comentario) {
        HistorialTarea evento = new HistorialTarea();
        evento.setTareaId(tarea.getId());
        evento.setIntento(tarea.getIntentoActual());
        evento.setAccion(accion);
        evento.setActorId(acceso.usuarioId());
        evento.setComentario(comentario);
        evento.setEvidenciaUrl(tarea.getEvidenciaUrl());
        evento.setFecha(java.time.LocalDateTime.now(java.time.Clock.systemUTC()));
        historialRepository.save(evento);
    }

    // ==========================================
    // ELIMINAR TAREA
    // ==========================================

    @Transactional
    public void eliminar(Long id) {

        exigirAdministrador();

        Tarea tarea = buscar(id);

        if (historialRepository.existsByTareaId(id) ||
                (tarea.getEvidenciaUrl() != null && !tarea.getEvidenciaUrl().isBlank())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede borrar una tarea con evidencia o historial: conservar trazabilidad");
        }
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
