package com.example.clientes_venta.Gasto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    List<Gasto> findByTienda_IdAndFechaOrderByCreatedAtDesc(Long tiendaId, LocalDate fecha);

    @Query("""
        select coalesce(sum(g.total), 0)
        from Gasto g
        where g.tienda.id = :tiendaId
          and g.fecha = :fecha
    """)
    BigDecimal totalGastosDelDia(@Param("tiendaId") Long tiendaId, @Param("fecha") LocalDate fecha);

    @Query("""
        select g.proveedor as proveedor, coalesce(sum(g.total), 0) as total
        from Gasto g
        where g.tienda.id = :tiendaId
          and g.fecha = :fecha
        group by g.proveedor
        order by coalesce(sum(g.total), 0) desc
    """)
    List<TotalProveedorRow> totalesPorProveedor(@Param("tiendaId") Long tiendaId, @Param("fecha") LocalDate fecha);

    interface TotalProveedorRow {
        String getProveedor();
        BigDecimal getTotal();
    }
}