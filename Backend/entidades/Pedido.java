package entidades;

public class Pedido {
    private int id_pedido;
    private Integer id_mesero;
    private Integer id_cocina;
    private Integer id_administrador;
    private Integer id_mesa;
    private String tipo_pedido;
    private String nombre_cliente;
    private String estado;
    private double total;
    private String metodo_pago;
    private String fecha_creacion;
    private String fecha_cierre;

    // Relaciones con las entidades completas
    private Mesero mesero;
    private PersonalCocina cocina;
    private Administrador administrador;
    private Mesa mesa;

    public Pedido() {
    }

    public Pedido(int id_pedido, Integer id_mesero, Integer id_cocina, Integer id_administrador,
            Integer id_mesa, String tipo_pedido, String nombre_cliente, String estado,
            double total, String metodo_pago, String fecha_creacion, String fecha_cierre) {
        this.id_pedido = id_pedido;
        this.id_mesero = id_mesero;
        this.id_cocina = id_cocina;
        this.id_administrador = id_administrador;
        this.id_mesa = id_mesa;
        this.tipo_pedido = tipo_pedido;
        this.nombre_cliente = nombre_cliente;
        this.estado = estado;
        this.total = total;
        this.metodo_pago = metodo_pago;
        this.fecha_creacion = fecha_creacion;
        this.fecha_cierre = fecha_cierre;
    }

    // Getters y Setters de los identificadores y atributos básicos
    public int getId_pedido() {
        return id_pedido;
    }

    public void setId_pedido(int id_pedido) {
        this.id_pedido = id_pedido;
    }

    public Integer getId_mesero() {
        return id_mesero;
    }

    public void setId_mesero(Integer id_mesero) {
        this.id_mesero = id_mesero;
    }

    public Integer getId_cocina() {
        return id_cocina;
    }

    public void setId_cocina(Integer id_cocina) {
        this.id_cocina = id_cocina;
    }

    public Integer getId_administrador() {
        return id_administrador != null ? id_administrador : 0;
    }

    public void setId_administrador(Integer id_administrador) {
        this.id_administrador = id_administrador;
    }

    public Integer getId_mesa() {
        return id_mesa;
    }

    public void setId_mesa(Integer id_mesa) {
        this.id_mesa = id_mesa;
    }

    public String getTipo_pedido() {
        return tipo_pedido;
    }

    public void setTipo_pedido(String tipo_pedido) {
        this.tipo_pedido = tipo_pedido;
    }

    public String getNombre_cliente() {
        return nombre_cliente;
    }

    public void setNombre_cliente(String nombre_cliente) {
        this.nombre_cliente = nombre_cliente;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getMetodo_pago() {
        return metodo_pago;
    }

    public void setMetodo_pago(String metodo_pago) {
        this.metodo_pago = metodo_pago;
    }

    public String getFecha_creacion() {
        return fecha_creacion;
    }

    public void setFecha_creacion(String fecha_creacion) {
        this.fecha_creacion = fecha_creacion;
    }

    public String getFecha_cierre() {
        return fecha_cierre;
    }

    public void setFecha_cierre(String fecha_cierre) {
        this.fecha_cierre = fecha_cierre;
    }

    // Getters y Setters de las relaciones de entidades
    public Mesero getMesero() {
        return mesero;
    }

    public void setMesero(Mesero mesero) {
        this.mesero = mesero;
        if (mesero != null) {
            this.id_mesero = mesero.getId_mesero();
        }
    }

    public PersonalCocina getCocina() {
        return cocina;
    }

    public void setCocina(PersonalCocina cocina) {
        this.cocina = cocina;
        if (cocina != null) {
            this.id_cocina = cocina.getId_cocina();
        }
    }

    public Administrador getAdministrador() {
        return administrador;
    }

    public void setAdministrador(Administrador administrador) {
        this.administrador = administrador;
        if (administrador != null) {
            this.id_administrador = administrador.getId_administrador();
        }
    }

    public Mesa getMesa() {
        return mesa;
    }

    public void setMesa(Mesa mesa) {
        this.mesa = mesa;
        if (mesa != null) {
            this.id_mesa = mesa.getId_mesa();
        }
    }
}