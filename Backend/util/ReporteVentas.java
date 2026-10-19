package util;

/** Resultado del reporte de ventas (HU7, y costo de envío de domicilios). */
public class ReporteVentas {
    private final int cantidadPedidos;
    private final double totalVentas;
    private final double totalEnvios;

    public ReporteVentas(int cantidadPedidos, double totalVentas, double totalEnvios) {
        this.cantidadPedidos = cantidadPedidos;
        this.totalVentas = totalVentas;
        this.totalEnvios = totalEnvios;
    }

    public int getCantidadPedidos() {
        return cantidadPedidos;
    }

    /** Suma del total de los pedidos cerrados (incluye el costo de envío). */
    public double getTotalVentas() {
        return totalVentas;
    }

    /** Parte del total que corresponde a costos de envío de domicilios. */
    public double getTotalEnvios() {
        return totalEnvios;
    }

    @Override
    public String toString() {
        return "Pedidos cerrados: " + cantidadPedidos + " | Ventas: " + totalVentas
                + " | Envíos incluidos: " + totalEnvios;
    }
}