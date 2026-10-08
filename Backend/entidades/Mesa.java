package entidades;

public class Mesa {
    private int id_mesa;
    private int numero;
    private String estado;
    private int id_administrador;
    private Administrador administrador;

    public int getId_mesa() { return id_mesa; }
    public void setId_mesa(int id_mesa) { this.id_mesa = id_mesa; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getId_administrador() { return id_administrador; }
    public void setId_administrador(int id_administrador) { this.id_administrador = id_administrador; }

    public Administrador getAdministrador() { return administrador; }
    public void setAdministrador(Administrador administrador) { this.administrador = administrador; }
}
