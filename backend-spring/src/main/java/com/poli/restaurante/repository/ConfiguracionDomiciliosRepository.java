package com.poli.restaurante.repository;

import com.poli.restaurante.domain.ConfiguracionDomicilios;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionDomiciliosRepository extends JpaRepository<ConfiguracionDomicilios, Long> {

    Optional<ConfiguracionDomicilios> findByActivaTrue();
}
