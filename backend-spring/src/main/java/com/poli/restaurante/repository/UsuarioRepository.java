package com.poli.restaurante.repository;

import com.poli.restaurante.domain.Rol;
import com.poli.restaurante.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("select count(u) from Usuario u where u.rol = :rol and u.activo = true and u.id <> :excluirId")
    long contarActivosPorRolExcluyendo(Rol rol, Long excluirId);
}
