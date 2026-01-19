package com.example.clientes_venta.CompraExterna;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraExternaRepository extends JpaRepository<CompraExterna, Long> {

    List<CompraExterna> findByTienda_IdAndFechaOrderByCreatedAtDesc(Long tiendaId, LocalDate fecha);

    @Query("""
        select coalesce(sum(c.total), 0)
        from CompraExterna c
        where c.tienda.id = :tiendaId
          and c.fecha = :fecha
    """)
    BigDecimal totalDelDia(@Param("tiendaId") Long tiendaId, @Param("fecha") LocalDate fecha);
}