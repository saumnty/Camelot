package com.example.clientes_venta.Tienda;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


public interface TiendaRepository extends JpaRepository<Tienda, Long> {

    @Query("""
        SELECT t FROM Tienda t
        WHERE t.activa = true
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(t.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(t.categoria) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(t.ciudad) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY t.nombre ASC
    """)
    List<Tienda> buscar(@Param("q") String q);

    Optional<Tienda> findFirstByOwner_Id(Integer ownerId);

    @Query("""
        SELECT t FROM Tienda t
        WHERE t.activa = true
          AND t.tipo IN :tipos
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(t.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(t.categoria) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(t.ciudad) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY t.nombre ASC
    """)
    List<Tienda> buscarPorTipos(@Param("q") String q, @Param("tipos") List<TipoTienda> tipos);

    @Modifying
    @Transactional
    @Query("""
      UPDATE Tienda t
      SET t.nombre = :nombre,
          t.slug = :slug
      WHERE t.id = :id
    """)
    int updateNombreYSlug(@Param("id") Long id,
                          @Param("nombre") String nombre,
                          @Param("slug") String slug);

}