package com.example.clientes_venta.Compra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    // Mis compras (por usuario)
    List<Compra> findByUsuario_IdOrderByFechaDesc(Integer usuarioId);

    // Mis ventas (por tienda)
    List<Compra> findByTienda_IdOrderByFechaDesc(Long tiendaId);

    List<Compra> findByTienda_IdAndEstadoOrderByFechaDesc(Long tiendaId, EstadoCompra estado);

    // ==========================
    // 5A / 5B: Reportes de ventas
    // ==========================

    /**
     * Totales por día (ingresos) en un rango, solo CONFIRMADAS.
     * Devuelve: fecha (LocalDate) + ingresoDia (sum total)
     */
    @Query("""
        select
            function('date', c.fecha) as dia,
            coalesce(sum(c.total), 0) as ingresoDia
        from Compra c
        where c.tienda.id = :tiendaId
          and c.estado = :estado
          and c.fecha >= :desde
          and c.fecha < :hasta
        group by function('date', c.fecha)
        order by function('date', c.fecha) asc
    """)
    List<TotalDiaRow> totalesPorDia(
            @Param("tiendaId") Long tiendaId,
            @Param("estado") EstadoCompra estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    /**
     * Top productos por unidades (con ingresos, costo estimado y utilidad estimada).
     * Nota: costoProveedor null se toma como 0.
     */
    @Query("""
        select
            p.id as productoId,
            p.name as productoNombre,
            sum(ci.cantidad) as unidades,
            coalesce(sum(ci.cantidad * ci.precioUnitario), 0) as ingresos,
            coalesce(sum(ci.cantidad * coalesce(p.costoProveedor, 0)), 0) as costoEstimado,
            coalesce(sum(ci.cantidad * ci.precioUnitario), 0) - coalesce(sum(ci.cantidad * coalesce(p.costoProveedor, 0)), 0) as utilidadEstimada
        from Compra c
        join c.items ci
        join ci.producto p
        where c.tienda.id = :tiendaId
          and c.estado = :estado
          and c.fecha >= :desde
          and c.fecha < :hasta
        group by p.id, p.name
        order by sum(ci.cantidad) desc
    """)
    List<TopProductoRow> topProductos(
            @Param("tiendaId") Long tiendaId,
            @Param("estado") EstadoCompra estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    /**
     * Top categorías por unidades (con ingresos, costo estimado y utilidad estimada).
     * OJO: este query asume que Categoria tiene campo "name".
     * Si tu Categoria usa "nombre", cambia: cat.name -> cat.nombre
     */
    @Query("""
        select
            cat.id as categoriaId,
            cat.nombre as categoriaNombre,
            sum(ci.cantidad) as unidades,
            coalesce(sum(ci.cantidad * ci.precioUnitario), 0) as ingresos,
            coalesce(sum(ci.cantidad * coalesce(p.costoProveedor, 0)), 0) as costoEstimado,
            coalesce(sum(ci.cantidad * ci.precioUnitario), 0) - coalesce(sum(ci.cantidad * coalesce(p.costoProveedor, 0)), 0) as utilidadEstimada
        from Compra c
        join c.items ci
        join ci.producto p
        join p.categoria cat
        where c.tienda.id = :tiendaId
          and c.estado = :estado
          and c.fecha >= :desde
          and c.fecha < :hasta
        group by cat.id, cat.nombre
        order by sum(ci.cantidad) desc
    """)
    List<TopCategoriaRow> topCategorias(
            @Param("tiendaId") Long tiendaId,
            @Param("estado") EstadoCompra estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    // ==========================
    // Base para 5C (resumen diario)
    // ==========================

    @Query("""
        select coalesce(sum(c.total), 0)
        from Compra c
        where c.tienda.id = :tiendaId
          and c.estado = :estado
          and c.fecha >= :inicioDia
          and c.fecha < :finDia
    """)
    BigDecimal ingresosDelDia(
            @Param("tiendaId") Long tiendaId,
            @Param("estado") EstadoCompra estado,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia
    );

    @Query("""
        select coalesce(sum(ci.cantidad * coalesce(p.costoProveedor, 0)), 0)
        from Compra c
        join c.items ci
        join ci.producto p
        where c.tienda.id = :tiendaId
          and c.estado = :estado
          and c.fecha >= :inicioDia
          and c.fecha < :finDia
    """)
    BigDecimal costoEstimadoDelDia(
            @Param("tiendaId") Long tiendaId,
            @Param("estado") EstadoCompra estado,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia
    );

    // ===== Proyecciones =====
    interface TotalDiaRow {
        LocalDate getDia();
        BigDecimal getIngresoDia();
    }

    interface TopProductoRow {
        Long getProductoId();
        String getProductoNombre();
        Long getUnidades();
        BigDecimal getIngresos();
        BigDecimal getCostoEstimado();
        BigDecimal getUtilidadEstimada();
    }

    interface TopCategoriaRow {
        Long getCategoriaId();
        String getCategoriaNombre();
        Long getUnidades();
        BigDecimal getIngresos();
        BigDecimal getCostoEstimado();
        BigDecimal getUtilidadEstimada();
    }
}
