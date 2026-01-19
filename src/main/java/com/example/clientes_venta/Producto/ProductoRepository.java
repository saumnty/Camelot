package com.example.clientes_venta.Producto;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByName(String name);

    List<Producto> findByNameContainingIgnoreCase(String q);

    // ====== Catálogo general (como lo tenías) ======
    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.categoria c
        LEFT JOIN FETCH p.subcategoria s
        ORDER BY
          COALESCE(c.nombre, 'ZZZ'),
          COALESCE(s.nombre, 'ZZZ'),
          p.name
    """)
    List<Producto> findAllConCategoriaSubcategoriaOrdenado();

    // ====== Por tienda ======
    List<Producto> findByTienda_Id(Long tiendaId);

    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.categoria c
        LEFT JOIN FETCH p.subcategoria s
        WHERE p.tienda.id = :tiendaId
        ORDER BY
          COALESCE(c.nombre, 'ZZZ'),
          COALESCE(s.nombre, 'ZZZ'),
          p.name
    """)
    List<Producto> findByTiendaOrdenado(@Param("tiendaId") Long tiendaId);

    // ====== Marketplace (SOLO tiendas NORMALES) ======
    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.categoria c
        LEFT JOIN FETCH p.subcategoria s
        WHERE p.tienda.tipo = 'NORMAL'
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY
          COALESCE(c.nombre, 'ZZZ'),
          COALESCE(s.nombre, 'ZZZ'),
          p.name
    """)
    List<Producto> findAllMarketplace(@Param("q") String q);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Producto p where p.id = :id")
  Optional<Producto> findByIdForUpdate(@Param("id") Long id);

}
