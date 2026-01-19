package com.example.clientes_venta.Camara;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CamaraRepository extends JpaRepository<Camara, Long> {
    List<Camara> findByTiendaIdOrderByIdDesc(Long tiendaId);
}
