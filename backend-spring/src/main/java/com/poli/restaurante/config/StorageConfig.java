package com.poli.restaurante.config;

import com.poli.restaurante.storage.AlmacenArchivos;
import com.poli.restaurante.storage.AlmacenBaseDatos;
import com.poli.restaurante.storage.AlmacenLocal;
import com.poli.restaurante.storage.AlmacenR2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Elige dónde se guardan las fotos según STORAGE_PROVIDER (local por defecto). */
@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
    AlmacenArchivos almacenLocal(AppProperties props) {
        return new AlmacenLocal(props.storage().uploadsDir());
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "db")
    AlmacenArchivos almacenBaseDatos(NamedParameterJdbcTemplate jdbc) {
        return new AlmacenBaseDatos(jdbc);
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
    AlmacenArchivos almacenR2(AppProperties props) {
        AppProperties.Storage.R2 r2 = props.storage().r2();
        return new AlmacenR2(r2.endpoint(), r2.bucket(), r2.accessKeyId(), r2.secretAccessKey());
    }
}
