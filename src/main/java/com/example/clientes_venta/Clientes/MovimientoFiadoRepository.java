package com.example.clientes_venta.Clientes;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoFiadoRepository extends JpaRepository<MovimientoFiado, Long> {
    List<MovimientoFiado> findTop20ByClienteTienda_IdOrderByFechaDesc(Long clienteTiendaId);
}
