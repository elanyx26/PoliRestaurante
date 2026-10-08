package entidades;

public class Administrador {
    private int id_administrador;
    private String nombre;
    private String usuario;
    private String contrasena;

    public int getId_administrador() { return id_administrador; }
    public void setId_administrador(int id_administrador) { this.id_administrador = id_administrador; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}
