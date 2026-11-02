package com.poli.restaurante;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** El script de datos de ejemplo para la demo debe funcionar sobre el esquema real y poder repetirse sin duplicar. */
class DatosDemoSqlIT extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transacciones;

    @Test
    void elScriptCreaElMenuYLasMesasYEsRepetible() throws Exception {
        String script = Files.readString(Path.of("deploy", "datos-demo.sql"), StandardCharsets.UTF_8);
        TransactionTemplate tx = new TransactionTemplate(transacciones);

        tx.executeWithoutResult(estado -> {
            int categoriasAntes = contar("categorias");
            int productosAntes = contar("productos");
            int mesasAntes = contar("mesas");

            jdbc.execute(script);
            assertEquals(categoriasAntes + 5, contar("categorias"));
            assertEquals(productosAntes + 10, contar("productos"));
            assertEquals(mesasAntes + 6, contar("mesas"));
            // Cada producto quedó dentro de su categoría.
            assertEquals(3, jdbc.queryForObject("""
                    SELECT count(*) FROM productos p JOIN categorias c ON c.id = p.categoria_id WHERE c.nombre = 'Hamburguesas'
                    """, Integer.class));

            jdbc.execute(script); // segunda ejecución: no duplica
            assertEquals(categoriasAntes + 5, contar("categorias"));
            assertEquals(productosAntes + 10, contar("productos"));
            assertEquals(mesasAntes + 6, contar("mesas"));

            estado.setRollbackOnly(); // no deja datos en la base compartida de las pruebas
        });
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }
}
