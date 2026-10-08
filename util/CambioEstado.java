package util;

import java.time.LocalDateTime;

/** Registro de un cambio de estado de un pedido (RF-15, RNF-09, HU4). */
public class CambioEstado {
    private final int id_pedido;
    private final String estado;
    private final LocalDateTime fecha;

    public CambioEstado(int id_pedido, String estado, LocalDateTime fecha) {
        this.id_pedido = id_pedido;
        this.estado = estado;
        this.fecha = fecha;
    }

    public int getId_pedido() { return id_pedido; }
    public String getEstado() { return estado; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() {
        return fecha.format(Fechas.FORMATO) + " -> " + estado;
    }
}
