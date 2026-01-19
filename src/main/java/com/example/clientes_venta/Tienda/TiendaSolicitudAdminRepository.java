package com.example.clientes_venta.Tienda;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TiendaSolicitudAdminRepository extends JpaRepository<TiendaSolicitudAdmin, Long> {

    boolean existsByTienda_IdAndSolicitante_IdAndEstado(Long tiendaId, Integer solicitanteId, EstadoSolicitudAdmin estado);

    List<TiendaSolicitudAdmin> findByTienda_IdAndEstadoOrderByCreatedAtDesc(Long tiendaId, EstadoSolicitudAdmin estado);

    List<TiendaSolicitudAdmin> findByTienda_IdOrderByCreatedAtDesc(Long tiendaId);

    Optional<TiendaSolicitudAdmin> findByIdAndTienda_Id(Long solId, Long tiendaId);

    boolean existsBySolicitante_IdAndTienda_IdAndEstado(Long solicitanteId, Long tiendaId, EstadoSolicitudAdmin estado);
}
