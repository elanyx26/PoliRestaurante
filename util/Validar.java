package util;

public final class Validar {
    private Validar() { }

    public static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim();
    }

    public static void positivo(int valor, String campo) {
        if (valor <= 0) {
            throw new IllegalArgumentException("El campo '" + campo + "' debe ser mayor a cero");
        }
    }

    public static void noNegativo(int valor, String campo) {
        if (valor < 0) {
            throw new IllegalArgumentException("El campo '" + campo + "' no puede ser negativo");
        }
    }

    public static void noNulo(Object o, String nombre) {
        if (o == null) {
            throw new IllegalArgumentException(nombre + " no puede ser nulo");
        }
    }
}
