package com.poli.restaurante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class AutenticacionYSeguridadIT extends IntegrationTestBase {

    @Test
    void loginDelAdministradorInicialDevuelveTokenSinExponerElHash() throws Exception {
        String cuerpo = esperar(200, HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ADMIN_EMAIL, ADMIN_PASSWORD));
        assertNotNull(campo(cuerpo, "$.accessToken"));
        assertEquals("Bearer", campo(cuerpo, "$.tokenType"));
        assertEquals("ADMINISTRADOR", campo(cuerpo, "$.usuario.rol"));
        assertFalse(cuerpo.contains("passwordHash") || cuerpo.contains("password_hash"));
    }

    @Test
    void credencialesInvalidasDanLaMismaRespuestaParaUsuarioInexistenteYContrasenaErrada() throws Exception {
        String malaClave = esperar(401, HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"incorrecta\"}".formatted(ADMIN_EMAIL));
        String sinUsuario = esperar(401, HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"nadie@test.local\",\"password\":\"incorrecta\"}");
        String detalleMalaClave = campo(malaClave, "$.detail");
        String detalleSinUsuario = campo(sinUsuario, "$.detail");
        assertEquals(detalleMalaClave, detalleSinUsuario);
    }

    @Test
    void desactivarUnUsuarioInvalidaDeInmediatoSuTokenVigente() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        esperar(200, HttpMethod.GET, "/api/auth/me", mesero.token(), null);

        esperar(200, HttpMethod.PATCH, "/api/admin/usuarios/" + mesero.id() + "/estado", tokenAdmin(),
                "{\"activo\":false}");

        esperar(401, HttpMethod.GET, "/api/auth/me", mesero.token(), null);
        esperar(401, HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(mesero.email(), CLAVE));
    }

    @Test
    void sinTokenOConTokenManipuladoSeRechaza() throws Exception {
        esperar(401, HttpMethod.GET, "/api/menu/categorias", null, null);
        esperar(401, HttpMethod.GET, "/api/menu/categorias", "esto.no.es-un-jwt", null);
        String valido = tokenAdmin();
        esperar(401, HttpMethod.GET, "/api/menu/categorias", valido.substring(0, valido.length() - 3) + "abc", null);
    }

    @Test
    void tras_cinco_fallos_el_login_se_bloquea_incluso_con_la_clave_correcta() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        for (int i = 0; i < 5; i++) {
            esperar(401, HttpMethod.POST, "/api/auth/login", null,
                    "{\"email\":\"%s\",\"password\":\"mala-%d\"}".formatted(mesero.email(), i));
        }
        esperar(429, HttpMethod.POST, "/api/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(mesero.email(), CLAVE));
    }

    @Test
    void cadaRolSoloAccedeAloSuyo() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        Sesion cocina = crearUsuario("COCINA");
        String admin = tokenAdmin();

        esperar(403, HttpMethod.GET, "/api/admin/usuarios", mesero.token(), null);
        esperar(403, HttpMethod.GET, "/api/admin/reportes/ventas", cocina.token(), null);
        esperar(403, HttpMethod.POST, "/api/usuarios", mesero.token(),
                "{\"nombre\":\"X\",\"email\":\"x@test.local\",\"password\":\"%s\",\"rol\":\"MESERO\"}".formatted(CLAVE));
        esperar(403, HttpMethod.GET, "/api/pedidos/cocina", mesero.token(), null);
        esperar(403, HttpMethod.GET, "/api/mesas/disponibles", cocina.token(), null);
        esperar(403, HttpMethod.POST, "/api/pedidos", cocina.token(), pedidoSalon(1, 1, 1));
        esperar(403, HttpMethod.POST, "/api/pedidos", admin, pedidoSalon(1, 1, 1));

        esperar(200, HttpMethod.GET, "/api/menu/categorias", cocina.token(), null);
        esperar(200, HttpMethod.GET, "/api/admin/usuarios", admin, null);
    }

    @Test
    void laValidacionDevuelve400ConElDetalleDeCadaCampo() throws Exception {
        String cuerpo = esperar(400, HttpMethod.POST, "/api/auth/login", null, "{\"email\":\"no-es-correo\"}");
        assertNotNull(campo(cuerpo, "$.errores.email"));
        assertNotNull(campo(cuerpo, "$.errores.password"));
    }

    @Test
    void elUnicoAdministradorActivoNoPuedeDesactivarse() throws Exception {
        String admin = tokenAdmin();
        long adminId = ((Number) campo(esperar(200, HttpMethod.GET, "/api/auth/me", admin, null), "$.id")).longValue();
        esperar(422, HttpMethod.PATCH, "/api/admin/usuarios/" + adminId + "/estado", admin, "{\"activo\":false}");
    }

    @Test
    void noSePuedenRepetirCorreosAunqueCambieLaCapitalizacion() throws Exception {
        Sesion mesero = crearUsuario("MESERO");
        esperar(409, HttpMethod.POST, "/api/usuarios", tokenAdmin(),
                "{\"nombre\":\"Otro\",\"email\":\"%s\",\"password\":\"%s\",\"rol\":\"COCINA\"}"
                        .formatted(mesero.email().toUpperCase(), CLAVE));
    }
}
