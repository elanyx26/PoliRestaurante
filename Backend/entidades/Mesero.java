package entidades;

public class Mesero {
    private int id_mesero;
    private String nombre;
    private String usuario;
    private String contrasena;

    public int getId_mesero() { return id_mesero; }
    public void setId_mesero(int id_mesero) { this.id_mesero = id_mesero; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}
