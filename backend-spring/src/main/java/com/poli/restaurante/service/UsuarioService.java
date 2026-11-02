package com.poli.restaurante.service;

import com.poli.restaurante.domain.Rol;
import com.poli.restaurante.domain.Usuario;
import com.poli.restaurante.exception.BusinessRuleException;
import com.poli.restaurante.exception.ConflictException;
import com.poli.restaurante.exception.ResourceNotFoundException;
import com.poli.restaurante.repository.UsuarioRepository;
import com.poli.restaurante.web.dto.PageResponse;
import com.poli.restaurante.web.dto.UsuarioDtos.ActualizarUsuarioRequest;
import com.poli.restaurante.web.dto.UsuarioDtos.CrearUsuarioRequest;
import com.poli.restaurante.web.dto.UsuarioDtos.UsuarioResponse;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioResponse crear(CrearUsuarioRequest req) {
        String email = normalizar(req.email());
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe un usuario con el correo " + email);
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRol(req.rol());
        return UsuarioResponse.de(usuarios.save(u));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(Rol rol, Boolean activo, String q, Pageable pageable) {
        Specification<Usuario> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (rol != null) {
                ps.add(cb.equal(root.get("rol"), rol));
            }
            if (activo != null) {
                ps.add(cb.equal(root.get("activo"), activo));
            }
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("email")), patron)));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        return PageResponse.de(usuarios.findAll(spec, pageable), UsuarioResponse::de);
    }

    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest req) {
        Usuario u = buscar(id);
        String email = normalizar(req.email());
        if (usuarios.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("Ya existe un usuario con el correo " + email);
        }
        if (u.getRol() == Rol.ADMINISTRADOR && req.rol() != Rol.ADMINISTRADOR) {
            exigirOtroAdministradorActivo(u);
        }
        u.setNombre(req.nombre().trim());
        u.setEmail(email);
        u.setRol(req.rol());
        return UsuarioResponse.de(usuarios.saveAndFlush(u));
    }

    public UsuarioResponse cambiarEstado(Long id, boolean activo) {
        Usuario u = buscar(id);
        if (!activo && u.getRol() == Rol.ADMINISTRADOR && u.isActivo()) {
            exigirOtroAdministradorActivo(u);
        }
        u.setActivo(activo);
        return UsuarioResponse.de(usuarios.saveAndFlush(u));
    }

    public void cambiarContrasena(Long id, String nuevaContrasena) {
        Usuario u = buscar(id);
        u.setPasswordHash(encoder.encode(nuevaContrasena));
    }

    private void exigirOtroAdministradorActivo(Usuario u) {
        if (u.isActivo() && usuarios.contarActivosPorRolExcluyendo(Rol.ADMINISTRADOR, u.getId()) == 0) {
            throw new BusinessRuleException("Debe existir al menos un administrador activo");
        }
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ResourceNotFoundException.de("Usuario", id));
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
