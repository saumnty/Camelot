package com.example.clientes_venta.Compra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraItemRepository extends JpaRepository<CompraItem, Long> {

    // Todos los items de una compra (útil si luego quieres recalcular total)
    List<CompraItem> findByCompra_Id(Long compraId);

    // Eliminar todos los items de una compra (opcional)
    void deleteByCompra_Id(Long compraId);
}
