package com.example.clientes_venta.Carrito;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    Optional<Carrito> findByUsuario_IdAndActivoTrue(Integer usuarioId);
}
