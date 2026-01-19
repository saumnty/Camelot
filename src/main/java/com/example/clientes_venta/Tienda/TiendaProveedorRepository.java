package com.example.clientes_venta.Tienda;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TiendaProveedorRepository extends JpaRepository<TiendaProveedor, TiendaProveedorId> {

    List<TiendaProveedor> findByTienda_IdAndActivoTrueOrderByCreadoEnDesc(Long tiendaId);

    List<TiendaProveedor> findByProveedor_IdAndActivoTrueOrderByCreadoEnDesc(Long proveedorId);

    Optional<TiendaProveedor> findByTienda_IdAndProveedor_Id(Long tiendaId, Long proveedorId);
}
