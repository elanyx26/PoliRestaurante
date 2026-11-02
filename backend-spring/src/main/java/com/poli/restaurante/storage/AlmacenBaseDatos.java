package com.poli.restaurante.storage;

import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Guarda las fotos en la propia base de datos (tabla imagenes). No necesita ningún servicio extra ni tarjeta, y
 * sobrevive a reinicios de servidores con disco efímero. Pensado para pocas fotos pequeñas (máx. 2 MB cada una).
 */
public class AlmacenBaseDatos implements AlmacenArchivos {

    private final NamedParameterJdbcTemplate jdbc;

    public AlmacenBaseDatos(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(String nombre, byte[] datos, String tipoContenido) {
        jdbc.update("INSERT INTO imagenes (nombre, tipo, datos) VALUES (:nombre, :tipo, :datos)",
                new MapSqlParameterSource().addValue("nombre", nombre).addValue("tipo", tipoContenido).addValue("datos", datos));
    }

    @Override
    public Optional<Resource> leer(String nombre) {
        List<byte[]> filas = jdbc.query("SELECT datos FROM imagenes WHERE nombre = :nombre",
                new MapSqlParameterSource("nombre", nombre), (rs, i) -> rs.getBytes("datos"));
        return filas.isEmpty() ? Optional.empty() : Optional.of(new ByteArrayResource(filas.get(0)));
    }

    @Override
    public void borrar(String nombre) {
        jdbc.update("DELETE FROM imagenes WHERE nombre = :nombre", new MapSqlParameterSource("nombre", nombre));
    }
}
