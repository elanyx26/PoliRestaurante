package util;

/**
 * Valores permitidos para los campos de tipo String del modelo
 * (estados de pedido, tipos de pedido, estados de mesa, etc.),
 * tomados de las historias de usuario del documento.
 */
public final class Constantes {
    private Constantes() { }

    // Estados del pedido (HU4, HU9, HU15, HU23)
    public static final String PENDIENTE = "Pendiente";
    public static final String EN_PREPARACION = "En preparación";
    public static final String LISTO = "Listo";
    public static final String ENTREGADO = "Entregado";
    public static final String CANCELADO = "Cancelado";
    public static final String CERRADO = "Cerrado";

    // Tipos de pedido (RF-16, RF-17, RF-19)
    public static final String TIPO_MESA = "En mesa";
    public static final String TIPO_LLEVAR = "Para llevar";
    public static final String TIPO_DOMICILIO = "A domicilio";

    // Estados de mesa (HU6)
    public static final String MESA_LIBRE = "Libre";
    public static final String MESA_OCUPADA = "Ocupada";

    // Disponibilidad de producto (HU5)
    public static final String DISPONIBLE = "Disponible";
    public static final String NO_DISPONIBLE = "No disponible";

    // Roles (HU8, HU14)
    public static final String ROL_MESERO = "mesero";
    public static final String ROL_COCINA = "cocina";
    public static final String ROL_ADMINISTRADOR = "administrador";
}
