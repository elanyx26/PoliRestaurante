package com.poli.restaurante;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

/** Las fotos viven en la tabla "imagenes" (STORAGE_PROVIDER=db): sirve en servidores con disco efímero. */
@TestPropertySource(properties = "app.storage.provider=db")
class ImagenesEnBaseDatosIT extends IntegrationTestBase {

    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==");

    @Autowired
    private JdbcTemplate jdbc;

    private MvcResult subir(long producto, String nombre) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, "/api/admin/productos/" + producto + "/imagen")
                .file(new MockMultipartFile("archivo", nombre, "image/png", PNG_1X1))
                .header("Authorization", "Bearer " + tokenAdmin())).andReturn();
    }

    private int filas(String url) {
        String nombre = url.substring(url.lastIndexOf('/') + 1);
        return jdbc.queryForObject("SELECT count(*) FROM imagenes WHERE nombre = ?", Integer.class, nombre);
    }

    @Test
    void laFotoSeGuardaEnLaBaseYSeSirveSinToken() throws Exception {
        long producto = crearProducto("9000");
        MvcResult subida = subir(producto, "plato.png");
        assertEquals(200, estadoHttp(subida), subida.getResponse().getContentAsString());
        String url = campo(subida.getResponse().getContentAsString(), "$.imagenUrl");

        assertEquals(1, filas(url));
        MvcResult imagen = llamar(HttpMethod.GET, url, null, null);
        assertEquals(200, estadoHttp(imagen));
        assertEquals("image/png", imagen.getResponse().getContentType());
        assertArrayEquals(PNG_1X1, imagen.getResponse().getContentAsByteArray());
    }

    @Test
    void reemplazarQuitarOBorrarElProductoLimpiaLaTabla() throws Exception {
        long producto = crearProducto("9000");
        String primera = campo(subir(producto, "a.png").getResponse().getContentAsString(), "$.imagenUrl");
        String segunda = campo(subir(producto, "b.png").getResponse().getContentAsString(), "$.imagenUrl");
        assertNotEquals(primera, segunda);
        assertEquals(0, filas(primera));
        assertEquals(1, filas(segunda));
        assertEquals(404, estadoHttp(llamar(HttpMethod.GET, primera, null, null)));

        esperar(204, HttpMethod.DELETE, "/api/admin/productos/" + producto + "/imagen", tokenAdmin(), null);
        assertEquals(0, filas(segunda));

        String tercera = campo(subir(producto, "c.png").getResponse().getContentAsString(), "$.imagenUrl");
        esperar(204, HttpMethod.DELETE, "/api/admin/productos/" + producto, tokenAdmin(), null);
        assertEquals(0, filas(tercera));
    }

    @Test
    void unaImagenInexistenteDevuelve404() throws Exception {
        assertEquals(404, estadoHttp(llamar(HttpMethod.GET,
                "/api/public/imagenes/11111111-1111-1111-1111-111111111111.png", null, null)));
    }
}
