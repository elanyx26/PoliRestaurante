package com.poli.restaurante.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/** Guarda en una carpeta del servidor. Solo sirve si el disco es persistente (desarrollo local o un VPS). */
public class AlmacenLocal implements AlmacenArchivos {

    private final Path directorio;

    public AlmacenLocal(String carpeta) {
        this.directorio = Path.of(carpeta).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de imágenes " + directorio, e);
        }
    }

    @Override
    public void guardar(String nombre, byte[] datos, String tipoContenido) {
        try {
            Files.write(resolver(nombre), datos);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
    }

    @Override
    public Optional<Resource> leer(String nombre) {
        Path archivo = resolver(nombre);
        return Files.isRegularFile(archivo) ? Optional.of(new FileSystemResource(archivo)) : Optional.empty();
    }

    @Override
    public void borrar(String nombre) {
        try {
            Files.deleteIfExists(resolver(nombre));
        } catch (IOException e) {
            // Un archivo huérfano no debe tumbar la operación del usuario.
        }
    }

    /** Defensa adicional: aunque el nombre ya se valida antes, nunca se sale de la carpeta. */
    private Path resolver(String nombre) {
        Path archivo = directorio.resolve(nombre).normalize();
        if (!archivo.startsWith(directorio)) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
        return archivo;
    }
}
