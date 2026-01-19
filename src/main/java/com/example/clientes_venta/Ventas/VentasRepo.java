package com.example.clientes_venta.Ventas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VentasRepo extends JpaRepository<Ventas, Integer> {
    List<Ventas> findByTienda_IdAndEstadoOrderByFechaDesc(Long tiendaId, Estado estado);
}