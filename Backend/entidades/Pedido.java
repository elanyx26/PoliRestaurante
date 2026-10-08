package entidades;

public class Pedido {
    private int id_pedido;
    private int id_mesero;
    private int id_cocina;
    private int id_administrador;
    private int id_mesa;
    private String tipo_pedido;
    private String nombre_cliente;
    private String estado;
    private int total;
    private String metodo_pago;
    private String fecha_creacion;
    private String fecha_cierre;
    private Mesero mesero;
    private PersonalCocina cocina;
    private Administrador administrador;
    private Mesa mesa;

    public int getId_pedido() { return id_pedido; }
    public void setId_pedido(int id_pedido) { this.id_pedido = id_pedido; }

    public int getId_mesero() { return id_mesero; }
    public void setId_mesero(int id_mesero) { this.id_mesero = id_mesero; }

    public int getId_cocina() { return id_cocina; }
    public void setId_cocina(int id_cocina) { this.id_cocina = id_cocina; }

    public int getId_administrador() { return id_administrador; }
    public void setId_administrador(int id_administrador) { this.id_administrador = id_administrador; }

    public int getId_mesa() { return id_mesa; }
    public void setId_mesa(int id_mesa) { this.id_mesa = id_mesa; }

    public String getTipo_pedido() { return tipo_pedido; }
    public void setTipo_pedido(String tipo_pedido) { this.tipo_pedido = tipo_pedido; }

    public String getNombre_cliente() { return nombre_cliente; }
    public void setNombre_cliente(String nombre_cliente) { this.nombre_cliente = nombre_cliente; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public String getMetodo_pago() { return metodo_pago; }
    public void setMetodo_pago(String metodo_pago) { this.metodo_pago = metodo_pago; }

    public String getFecha_creacion() { return fecha_creacion; }
    public void setFecha_creacion(String fecha_creacion) { this.fecha_creacion = fecha_creacion; }

    public String getFecha_cierre() { return fecha_cierre; }
    public void setFecha_cierre(String fecha_cierre) { this.fecha_cierre = fecha_cierre; }

    public Mesero getMesero() { return mesero; }
    public void setMesero(Mesero mesero) { this.mesero = mesero; }

    public PersonalCocina getCocina() { return cocina; }
    public void setCocina(PersonalCocina cocina) { this.cocina = cocina; }

    public Administrador getAdministrador() { return administrador; }
    public void setAdministrador(Administrador administrador) { this.administrador = administrador; }

    public Mesa getMesa() { return mesa; }
    public void setMesa(Mesa mesa) { this.mesa = mesa; }
}
