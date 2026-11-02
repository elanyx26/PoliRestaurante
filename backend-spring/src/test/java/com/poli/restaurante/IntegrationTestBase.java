package com.poli.restaurante;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import com.jayway.jsonpath.JsonPath;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base de las pruebas de integración: levanta la aplicación completa contra un PostgreSQL real embebido
 * (sin Docker), de modo que Flyway, JPA, la seguridad y las restricciones SQL se ejercitan de verdad.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String ADMIN_EMAIL = "admin@test.local";
    protected static final String ADMIN_PASSWORD = "AdminPruebas-123";
    protected static final String CLAVE = "Clave-Segura-1";

    private static final EmbeddedPostgres POSTGRES;
    private static final AtomicInteger SECUENCIA = new AtomicInteger(1000);

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
    }

    @Autowired
    protected MockMvc mvc;

    /** Usuario de prueba ya autenticado. */
    protected record Sesion(long id, String email, String token) {
    }

    // ------------------------------------------------------------------ HTTP

    protected MvcResult llamar(HttpMethod metodo, String url, String token, String json) throws Exception {
        MockHttpServletRequestBuilder req = request(metodo, url);
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(req).andReturn();
    }

    /** Llama y exige el código HTTP esperado, devolviendo el cuerpo (útil para ver el error si falla). */
    protected String esperar(int estado, HttpMethod metodo, String url, String token, String json) throws Exception {
        MvcResult r = llamar(metodo, url, token, json);
        String cuerpo = r.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(estado, r.getResponse().getStatus(),
                metodo + " " + url + " -> " + cuerpo);
        return cuerpo;
    }

    protected static <T> T campo(String json, String ruta) {
        return JsonPath.read(json, ruta);
    }

    protected static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    protected static BigDecimal numero(String json, String ruta) {
        return new BigDecimal(String.valueOf((Object) JsonPath.read(json, ruta)));
    }

    // -------------------------------------------------------------- fixtures

    protected String tokenAdmin() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
        String cuerpo = esperar(200, HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
        return campo(cuerpo, "$.accessToken");
    }

    protected Sesion crearUsuario(String rol) throws Exception {
        String email = rol.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        String cuerpo = esperar(201, HttpMethod.POST, "/api/usuarios", tokenAdmin(),
                "{\"nombre\":\"Usuario %s\",\"email\":\"%s\",\"password\":\"%s\",\"rol\":\"%s\"}"
                        .formatted(rol, email, CLAVE, rol));
        return new Sesion(id(cuerpo), email, login(email, CLAVE));
    }

    protected long crearMesa() throws Exception {
        int numero = SECUENCIA.incrementAndGet();
        return id(esperar(201, HttpMethod.POST, "/api/admin/mesas", tokenAdmin(),
                "{\"numero\":%d,\"capacidad\":4}".formatted(numero)));
    }

    protected long crearCategoria() throws Exception {
        return id(esperar(201, HttpMethod.POST, "/api/admin/categorias", tokenAdmin(),
                "{\"nombre\":\"Categoria %s\"}".formatted(UUID.randomUUID())));
    }

    protected long crearProducto(String precio) throws Exception {
        return crearProducto(crearCategoria(), precio);
    }

    protected long crearProducto(long categoriaId, String precio) throws Exception {
        return id(esperar(201, HttpMethod.POST, "/api/admin/productos", tokenAdmin(),
                "{\"categoriaId\":%d,\"nombre\":\"Producto %s\",\"precio\":%s}"
                        .formatted(categoriaId, UUID.randomUUID(), precio)));
    }

    protected String pedidoSalon(long mesaId, long productoId, int cantidad) {
        return "{\"tipo\":\"SALON\",\"mesaId\":%d,\"items\":[{\"productoId\":%d,\"cantidad\":%d,\"notas\":\"sin cebolla\"}]}"
                .formatted(mesaId, productoId, cantidad);
    }

    protected long crearPedidoSalon(Sesion mesero, long productoId, int cantidad) throws Exception {
        return id(esperar(201, HttpMethod.POST, "/api/pedidos", mesero.token(),
                pedidoSalon(crearMesa(), productoId, cantidad)));
    }

    protected String estado(String estado) {
        return "{\"estado\":\"%s\"}".formatted(estado);
    }

    protected static void assertTotal(String json, String esperado) {
        org.junit.jupiter.api.Assertions.assertEquals(0, numero(json, "$.total").compareTo(new BigDecimal(esperado)),
                "total esperado " + esperado + " pero fue " + numero(json, "$.total"));
    }

    protected static int estadoHttp(MvcResult r) {
        return r.getResponse().getStatus();
    }
}
