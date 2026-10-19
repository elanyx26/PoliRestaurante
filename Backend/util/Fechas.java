package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Las fechas del modelo son String; aquí se usa un formato fijo y ordenable. */
public final class Fechas {
    private Fechas() { }

    public static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static String ahora() { return LocalDateTime.now().format(FORMATO); }

    public static LocalDateTime parsear(String fecha) {
        return fecha == null ? null : LocalDateTime.parse(fecha, FORMATO);
    }
}
