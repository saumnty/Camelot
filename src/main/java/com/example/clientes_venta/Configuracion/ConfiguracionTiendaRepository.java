package com.example.clientes_venta.Configuracion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionTiendaRepository extends JpaRepository<ConfiguracionTienda, Long> {
    Optional<ConfiguracionTienda> findByTienda_Id(Long tiendaId);
}
