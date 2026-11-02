package com.poli.restaurante;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

class MenuPublicoEImagenesIT extends IntegrationTestBase {

    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==");

    private MvcResult subir(long productoId, String token, String nombre, byte[] datos) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, "/api/admin/productos/" + productoId + "/imagen")
                .file(new MockMultipartFile("archivo", nombre, "image/png", datos))
                .header("Authorization", "Bearer " + token)).andReturn();
    }

    @Test
    void elMenuPublicoNoRequiereTokenYSoloMuestraLoDisponible() throws Exception {
        long categoria = crearCategoria();
        long visible = crearProducto(categoria, "9000");
        long oculto = crearProducto(categoria, "7000");
        long categoriaInactiva = crearCategoria();
        long enInactiva = crearProducto(categoriaInactiva, "5000");
        String admin = tokenAdmin();

        esperar(200, HttpMethod.PUT, "/api/admin/productos/" + oculto, admin,
                "{\"categoriaId\":%d,\"nombre\":\"Agotado\",\"precio\":7000,\"disponible\":false}".formatted(categoria));
        esperar(200, HttpMethod.PUT, "/api/admin/categorias/" + categoriaInactiva, admin,
                "{\"nombre\":\"Inactiva %d\",\"activa\":false}".formatted(categoriaInactiva));

        String menu = esperar(200, HttpMethod.GET, "/api/public/menu", null, null);
        List<Integer> productos = campo(menu, "$.categorias[*].productos[*].id");
        assertTrue(productos.stream().anyMatch(id -> id.longValue() == visible));
        assertFalse(productos.stream().anyMatch(id -> id.longValue() == oculto));
        assertFalse(productos.stream().anyMatch(id -> id.longValue() == enInactiva));
        // Solo datos de presentación.
        assertFalse(menu.contains("disponible") || menu.contains("categoriaId"));
    }

    @Test
    void elAdministradorSubeUnaFotoYCualquieraLaPuedeVerSinToken() throws Exception {
        long producto = crearProducto("9000");
        MvcResult subida = subir(producto, tokenAdmin(), "mi-plato.png", PNG_1X1);
        assertEquals(200, subida.getResponse().getStatus(), subida.getResponse().getContentAsString());

        String url = campo(subida.getResponse().getContentAsString(), "$.imagenUrl");
        assertTrue(url.startsWith("/api/public/imagenes/") && url.endsWith(".png"));
        assertFalse(url.contains("mi-plato"), "el nombre en disco no debe depender del archivo del usuario");

        MvcResult imagen = llamar(HttpMethod.GET, url, null, null);
        assertEquals(200, imagen.getResponse().getStatus());
        assertEquals("image/png", imagen.getResponse().getContentType());
        assertArrayEquals(PNG_1X1, imagen.getResponse().getContentAsByteArray());
        assertNotNull(imagen.getResponse().getHeader("Cache-Control"));

        // La portada ya la incluye.
        String menu = esperar(200, HttpMethod.GET, "/api/public/menu", null, null);
        List<String> urls = campo(menu, "$.categorias[*].productos[?(@.id==" + producto + ")].imagenUrl");
        assertEquals(List.of(url), urls);
    }

    @Test
    void reemplazarOQuitarLaFotoBorraLaAnterior() throws Exception {
        long producto = crearProducto("9000");
        String admin = tokenAdmin();
        String primera = campo(subir(producto, admin, "a.png", PNG_1X1).getResponse().getContentAsString(), "$.imagenUrl");
        String segunda = campo(subir(producto, admin, "b.png", PNG_1X1).getResponse().getContentAsString(), "$.imagenUrl");

        assertFalse(primera.equals(segunda));
        assertEquals(404, estadoHttp(llamar(HttpMethod.GET, primera, null, null)));
        assertEquals(200, estadoHttp(llamar(HttpMethod.GET, segunda, null, null)));

        esperar(204, HttpMethod.DELETE, "/api/admin/productos/" + producto + "/imagen", admin, null);
        assertEquals(404, estadoHttp(llamar(HttpMethod.GET, segunda, null, null)));
    }

    @Test
    void soloSeAceptanImagenesRealesDeTamanoRazonable() throws Exception {
        long producto = crearProducto("9000");
        String admin = tokenAdmin();

        // Un texto con extensión .png no es una imagen: el tipo se detecta por los bytes.
        assertEquals(422, estadoHttp(subir(producto, admin, "falso.png", "<script>alert(1)</script>".getBytes())));
        // SVG nunca se acepta (puede contener scripts).
        assertEquals(422, estadoHttp(subir(producto, admin, "logo.svg", "<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes())));
        // Más de 2 MB.
        byte[] grande = new byte[2 * 1024 * 1024 + 10];
        System.arraycopy(PNG_1X1, 0, grande, 0, 8);
        assertEquals(422, estadoHttp(subir(producto, admin, "grande.png", grande)));
        assertEquals(404, esperarEstadoDeProducto(producto));
    }

    @Test
    void soloElAdministradorSubeFotosYLosNombresInvalidosNoTocanElDisco() throws Exception {
        long producto = crearProducto("9000");
        Sesion mesero = crearUsuario("MESERO");
        assertEquals(403, estadoHttp(subir(producto, mesero.token(), "x.png", PNG_1X1)));
        assertEquals(401, estadoHttp(subir(producto, "token-falso", "x.png", PNG_1X1)));

        assertEquals(404, estadoHttp(llamar(HttpMethod.GET, "/api/public/imagenes/passwd", null, null)));
        assertEquals(404, estadoHttp(llamar(HttpMethod.GET, "/api/public/imagenes/..passwd.png", null, null)));
        assertEquals(404, estadoHttp(llamar(HttpMethod.GET,
                "/api/public/imagenes/00000000-0000-0000-0000-000000000000.png", null, null)));
    }

    @Test
    void borrarUnProductoBorraSuFoto() throws Exception {
        long producto = crearProducto("9000");
        String admin = tokenAdmin();
        String url = campo(subir(producto, admin, "a.png", PNG_1X1).getResponse().getContentAsString(), "$.imagenUrl");
        esperar(204, HttpMethod.DELETE, "/api/admin/productos/" + producto, admin, null);
        assertEquals(404, estadoHttp(llamar(HttpMethod.GET, url, null, null)));
    }

    /** El producto sigue sin foto tras los intentos rechazados. */
    private int esperarEstadoDeProducto(long producto) throws Exception {
        String lista = esperar(200, HttpMethod.GET, "/api/admin/productos?size=100&sort=id,desc", tokenAdmin(), null);
        List<Object> urls = campo(lista, "$.content[?(@.id==" + producto + ")].imagenUrl");
        return urls.isEmpty() ? 404 : 200;
    }
}
