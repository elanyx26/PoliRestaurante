package api;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Servidor REST de PoliRestaurante (módulo de administración).
 * Ejecutar:  java -cp "out:lib/*" api.ServidorRest        (Windows: "out;lib/*")
 * Puerto por defecto 8080; se cambia con -Dport=9090 o la variable de entorno PORT.
 */
public class ServidorRest {

    public static void main(String[] args) throws IOException {
        int puerto = Integer.parseInt(System.getProperty("port",
                System.getenv("PORT") != null ? System.getenv("PORT") : "8080"));

        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        registrar(servidor, "/api/categorias", new CategoriaRecurso());
        registrar(servidor, "/api/productos", new ProductoRecurso());
        registrar(servidor, "/api/mesas", new MesaRecurso());
        registrar(servidor, "/api/administradores", Usuarios.administradores());
        registrar(servidor, "/api/meseros", Usuarios.meseros());
        registrar(servidor, "/api/cocina", Usuarios.cocina());
        registrar(servidor, "/api/roles", new RolRecurso());
        registrar(servidor, "/api/pedidos", new PedidoRecurso());
        registrar(servidor, "/api/detalles", new DetallePedidoRecurso());
        servidor.setExecutor(Executors.newFixedThreadPool(8));
        servidor.start();

        System.out.println("PoliRestaurante REST escuchando en http://localhost:" + puerto + "/api");
        System.out.println("Recursos: categorias, productos, mesas, administradores, meseros, cocina, roles, pedidos, detalles");
    }

    private static void registrar(HttpServer s, String ruta, com.sun.net.httpserver.HttpHandler h) {
        s.createContext(ruta, h);
    }
}
