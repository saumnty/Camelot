package com.example.clientes_venta.Clientes;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByTienda_IdOrderByNombreAsc(Long tiendaId);
    Optional<Cliente> findByIdAndTienda_Id(Long id, Long tiendaId);
}
