package entidades;

public class Domicilio {
    private int id_domicilio;
    private int id_pedido;
    private String direccion;
    private String telefono;
    private double costo_envio; // double para soportar el valor DECIMAL de la BD
    private Pedido pedido;      // Objeto de relación con Pedido

    public Domicilio() {}

    public Domicilio(int id_domicilio, int id_pedido, String direccion, String telefono, double costo_envio) {
        this.id_domicilio = id_domicilio;
        this.id_pedido = id_pedido;
        this.direccion = direccion;
        this.telefono = telefono;
        this.costo_envio = costo_envio;
    }

    public int getId_domicilio() { return id_domicilio; }
    public void setId_domicilio(int id_domicilio) { this.id_domicilio = id_domicilio; }

    public int getId_pedido() { return id_pedido; }
    public void setId_pedido(int id_pedido) { this.id_pedido = id_pedido; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public double getCosto_envio() { return costo_envio; }
    public void setCosto_envio(double costo_envio) { this.costo_envio = costo_envio; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    
}