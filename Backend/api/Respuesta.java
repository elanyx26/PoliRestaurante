package api;

/** Resultado de un endpoint: código HTTP + cuerpo (se convierte a JSON). */
public final class Respuesta {
    public final int estado;
    public final Object cuerpo;

    private Respuesta(int estado, Object cuerpo) {
        this.estado = estado;
        this.cuerpo = cuerpo;
    }

    public static Respuesta ok(Object cuerpo) { return new Respuesta(200, cuerpo); }
    public static Respuesta creado(Object cuerpo) { return new Respuesta(201, cuerpo); }
    public static Respuesta sinContenido() { return new Respuesta(204, null); }
    public static Respuesta de(int estado, Object cuerpo) { return new Respuesta(estado, cuerpo); }
}
