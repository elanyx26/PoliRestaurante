package com.poli.restaurante.service;

import com.poli.restaurante.exception.BusinessRuleException;
import com.poli.restaurante.exception.ResourceNotFoundException;
import com.poli.restaurante.storage.AlmacenArchivos;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Valida y nombra las fotos de los productos; el guardado físico lo hace el AlmacenArchivos configurado
 * (disco, base de datos o R2). Seguridad:
 * <ul>
 *   <li>El tipo se detecta por los bytes iniciales, no por el nombre ni el Content-Type que envía el cliente.</li>
 *   <li>Solo JPG, PNG y WebP (nunca SVG, que puede contener scripts).</li>
 *   <li>El nombre lo genera el servidor (UUID): no hay path traversal ni colisiones.</li>
 * </ul>
 */
@Slf4j
@Service
public class AlmacenImagenes {

    /** Nombre válido: UUID + extensión permitida. Cualquier otro valor se rechaza antes de tocar el almacén. */
    private static final Pattern NOMBRE_VALIDO =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final long TAMANO_MAXIMO = 2L * 1024 * 1024;

    private final AlmacenArchivos almacen;

    public AlmacenImagenes(AlmacenArchivos almacen) {
        this.almacen = almacen;
    }

    /** Valida y guarda la imagen; devuelve el nombre generado. */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessRuleException("Debe enviar un archivo de imagen");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new BusinessRuleException("La imagen supera el máximo de 2 MB");
        }
        byte[] datos;
        try {
            datos = archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen", e);
        }
        String extension = detectarExtension(datos);
        if (extension == null) {
            throw new BusinessRuleException("Formato no permitido: use una imagen JPG, PNG o WebP");
        }
        String nombre = UUID.randomUUID() + "." + extension;
        almacen.guardar(nombre, datos, tipoDe(nombre).toString());
        return nombre;
    }

    public Resource cargar(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new ResourceNotFoundException("Imagen no encontrada");
        }
        return almacen.leer(nombre).orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada"));
    }

    public void borrar(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return;
        }
        try {
            almacen.borrar(nombre);
        } catch (RuntimeException e) {
            // Un archivo huérfano no debe tumbar la operación del usuario.
            log.warn("No se pudo borrar la imagen {}: {}", nombre, e.getMessage());
        }
    }

    public static MediaType tipoDe(String nombre) {
        if (nombre.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        return nombre.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
    }

    private static String detectarExtension(byte[] b) {
        if (empieza(b, JPEG)) {
            return "jpg";
        }
        if (empieza(b, PNG)) {
            return "png";
        }
        boolean riff = b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F';
        boolean webp = riff && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        return webp ? "webp" : null;
    }

    private static boolean empieza(byte[] datos, byte[] prefijo) {
        return datos.length >= prefijo.length && Arrays.equals(Arrays.copyOf(datos, prefijo.length), prefijo);
    }
}
