package entidades;

public class Administrador {
    private int id_administrador;
    private String nombre;
    private String usuario;
    private String contrasena;
    private String tipo_documento;
    private String numero_documento;
    private String correo;
    private String telefono;
    private String direccion;
    private boolean estado;

    // Constructor vacío
    public Administrador() {
    }

    // Constructor completo
    public Administrador(int id_administrador, String nombre, String usuario, String contrasena,
            String tipo_documento, String numero_documento, String correo,
            String telefono, String direccion, boolean estado) {
        this.id_administrador = id_administrador;
        this.nombre = nombre;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.tipo_documento = tipo_documento;
        this.numero_documento = numero_documento;
        this.correo = correo;
        this.telefono = telefono;
        this.direccion = direccion;
        this.estado = estado;
    }

    // Getters y Setters
    public int getId_administrador() {
        return id_administrador;
    }

    public void setId_administrador(int id_administrador) {
        this.id_administrador = id_administrador;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getTipo_documento() {
        return tipo_documento;
    }

    public void setTipo_documento(String tipo_documento) {
        this.tipo_documento = tipo_documento;
    }

    public String getNumero_documento() {
        return numero_documento;
    }

    public void setNumero_documento(String numero_documento) {
        this.numero_documento = numero_documento;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return id_administrador
                + " | Nombre: " + nombre
                + " | Usuario: " + usuario
                + " | Doc: " + tipo_documento + " " + numero_documento
                + " | Correo: " + correo
                + " | Tel: " + telefono
                + " | Dir: " + direccion
                + " | Estado: " + (estado ? "activo" : "inactivo");
    }
}