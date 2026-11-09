
package cl.duoc.tareas_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class ArchivoService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELES = 20_000_000L;

    private final Path directorio;

    public ArchivoService(
            @Value("${app.archivos.directorio:./uploads/evidencias}")
            String ruta) {

        this.directorio = Path.of(ruta)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de evidencias",
                    e
            );
        }
    }

    public String guardarImagen(MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes seleccionar una imagen"
            );
        }

        if (archivo.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "La imagen supera los 5 MB"
            );
        }

        String nombreOriginal = archivo.getOriginalFilename();

        if (nombreOriginal == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nombre de archivo invalido"
            );
        }

        String nombreMinusculas = nombreOriginal.toLowerCase();

        boolean jpeg = nombreMinusculas.endsWith(".jpg")
                || nombreMinusculas.endsWith(".jpeg");

        boolean png = nombreMinusculas.endsWith(".png");

        if (!jpeg && !png) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se permiten imagenes JPG o PNG"
            );
        }

        String formato = jpeg ? "jpg" : "png";
        String nombre = UUID.randomUUID() + "." + formato;

        Path destino = directorio.resolve(nombre).normalize();

        try {
            byte[] contenido = archivo.getBytes();

            if (contenido.length > MAX_BYTES) {
                throw new ResponseStatusException(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "La imagen supera los 5 MB"
                );
            }

            BufferedImage imagen;

            try (ByteArrayInputStream entrada =
                         new ByteArrayInputStream(contenido)) {

                imagen = ImageIO.read(entrada);
            }

            if (imagen == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El archivo no es una imagen valida"
                );
            }

            long pixeles = (long) imagen.getWidth()
                    * imagen.getHeight();

            if (pixeles > MAX_PIXELES) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Las dimensiones de la imagen son excesivas"
                );
            }

            // Reconstruir la imagen para no guardar
            // directamente los bytes originales.
            try (var salida = Files.newOutputStream(
                    destino,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {

                boolean escrita = ImageIO.write(
                        imagen,
                        formato.equals("jpg") ? "jpeg" : "png",
                        salida
                );

                if (!escrita) {
                    throw new IOException(
                            "No se pudo codificar la imagen"
                    );
                }
            }

            return nombre;

        } catch (ResponseStatusException e) {
            eliminar(nombre);
            throw e;

        } catch (IOException e) {
            eliminar(nombre);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar la imagen",
                    e
            );
        }
    }

    public Resource obtenerImagen(String nombre) {

        Path archivo = rutaSegura(nombre);

        if (!Files.isRegularFile(archivo)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Imagen no encontrada"
            );
        }

        return new FileSystemResource(archivo);
    }

    public void eliminar(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(rutaSegura(nombre));
        } catch (IOException ignored) {
            // Se registra o reintenta en una estrategia
            // de limpieza posterior si fuese necesario.
        }
    }

    private Path rutaSegura(String nombre) {

        if (nombre == null
                || !nombre.matches(
                "[0-9a-fA-F-]{36}\\.(jpg|png)")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nombre de archivo invalido"
            );
        }

        Path ruta = directorio.resolve(nombre).normalize();

        if (!ruta.startsWith(directorio)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ruta de archivo invalida"
            );
        }

        return ruta;
    }
}
