package entidades;

public class PersonalCocina {
    private int id_cocina;
    private String nombre;
    private String usuario;
    private String contrasena;

    public int getId_cocina() { return id_cocina; }
    public void setId_cocina(int id_cocina) { this.id_cocina = id_cocina; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}
