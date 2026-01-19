package com.example.clientes_venta.Subcategoria;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Long> {

    Optional<Subcategoria> findByCategoriaIdAndNombreIgnoreCase(Long categoriaId, String nombre);

    List<Subcategoria> findByCategoriaIdOrderByNombreAsc(Long categoriaId);

    @Query("""
        select s.nombre
        from Subcategoria s
        where lower(s.categoria.nombre) = lower(:categoriaNombre)
        order by s.nombre asc
    """)
    List<String> findNombresByCategoriaNombreIgnoreCase(@Param("categoriaNombre") String categoriaNombre);
}