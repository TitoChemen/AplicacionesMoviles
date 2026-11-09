package cl.duoc.huerta_service.exception;

public class RecursoNoEncontradoException
        extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
