package com.poli.restaurante.bootstrap;

import com.poli.restaurante.config.AppProperties;
import com.poli.restaurante.domain.Rol;
import com.poli.restaurante.domain.Usuario;
import com.poli.restaurante.repository.UsuarioRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Crea el administrador inicial (desde INITIAL_ADMIN_* del .env) solo cuando no existe ningún usuario. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdministradorInicial implements ApplicationRunner {

    private static final int LONGITUD_MINIMA = 8;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }
        AppProperties.InitialAdmin cfg = props.initialAdmin();
        if (vacio(cfg.email()) || vacio(cfg.password())) {
            log.warn("No hay usuarios y faltan INITIAL_ADMIN_EMAIL / INITIAL_ADMIN_PASSWORD: no se creó el administrador inicial");
            return;
        }
        if (cfg.password().length() < LONGITUD_MINIMA) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        Usuario admin = new Usuario();
        admin.setNombre(vacio(cfg.name()) ? "Administrador" : cfg.name().trim());
        admin.setEmail(cfg.email().trim().toLowerCase(Locale.ROOT));
        admin.setPasswordHash(encoder.encode(cfg.password()));
        admin.setRol(Rol.ADMINISTRADOR);
        usuarios.save(admin);
        log.info("Administrador inicial creado: {}", admin.getEmail());
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
