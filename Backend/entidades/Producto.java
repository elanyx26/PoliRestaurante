package entidades;

public class Producto {
    private int id_producto;
    private String nombre;
    private String descripcion;
    private double precio;
    private Integer id_categoria;
    private Integer id_administrador;
    private String disponibilidad;

    public Producto() {
    }

    public Producto(int id_producto, String nombre, String descripcion, double precio,
            Integer id_categoria, Integer id_administrador, String disponibilidad) {
        this.id_producto = id_producto;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.id_categoria = id_categoria;
        this.id_administrador = id_administrador;
        this.disponibilidad = disponibilidad;
    }

    public int getId_producto() {
        return id_producto;
    }

    public void setId_producto(int id_producto) {
        this.id_producto = id_producto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public Integer getId_categoria() {
        return id_categoria;
    }

    public void setId_categoria(Integer id_categoria) {
        this.id_categoria = id_categoria;
    }

    public Integer getId_administrador() {
        return id_administrador;
    }

    public void setId_administrador(Integer id_administrador) {
        this.id_administrador = id_administrador;
    }

    public String getDisponibilidad() {
        return disponibilidad;
    }

    public void setDisponibilidad(String disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    private Categoria categoria;
    private Administrador administrador;

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public void setAdministrador(Administrador administrador) {
        this.administrador = administrador;
    }

    public Categoria getCategoria() {
        return categoria;
    }
}