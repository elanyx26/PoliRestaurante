package entidades;

public class DetallePedido {
    private int id_detalle;
    private int id_pedido;
    private int id_producto;
    private int cantidad;
    private int precio_unitario;
    private String notas;
    private Pedido pedido;
    private Producto producto;

    public int getId_detalle() { return id_detalle; }
    public void setId_detalle(int id_detalle) { this.id_detalle = id_detalle; }

    public int getId_pedido() { return id_pedido; }
    public void setId_pedido(int id_pedido) { this.id_pedido = id_pedido; }

    public int getId_producto() { return id_producto; }
    public void setId_producto(int id_producto) { this.id_producto = id_producto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public int getPrecio_unitario() { return precio_unitario; }
    public void setPrecio_unitario(int precio_unitario) { this.precio_unitario = precio_unitario; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
}
