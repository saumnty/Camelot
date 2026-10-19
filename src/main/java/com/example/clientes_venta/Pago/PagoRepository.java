package com.example.clientes_venta.Pago;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findFirstByCompra_IdAndMetodoOrderByCreatedAtDesc(Long compraId, MetodoPago metodo);
}
