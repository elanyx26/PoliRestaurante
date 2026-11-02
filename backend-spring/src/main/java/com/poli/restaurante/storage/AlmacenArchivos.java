package com.poli.restaurante.storage;

import java.util.Optional;
import org.springframework.core.io.Resource;

/**
 * Dónde se guardan físicamente los archivos (fotos de productos). Las reglas de validación y los nombres los decide
 * AlmacenImagenes; las implementaciones solo guardan, leen y borran bytes por nombre.
 * Se elige con STORAGE_PROVIDER: local (disco), db (PostgreSQL) o r2 (Cloudflare R2 / S3).
 */
public interface AlmacenArchivos {

    void guardar(String nombre, byte[] datos, String tipoContenido);

    /** Vacío si el archivo no existe. */
    Optional<Resource> leer(String nombre);

    void borrar(String nombre);
}
