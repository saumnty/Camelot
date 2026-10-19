package com.example.clientes_venta.Tienda;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TiendaSolicitudAdminRepository extends JpaRepository<TiendaSolicitudAdmin, Long> {

    boolean existsByTienda_IdAndSolicitante_IdAndEstado(Long tiendaId, Integer solicitanteId, EstadoSolicitudAdmin estado);

    List<TiendaSolicitudAdmin> findByTienda_IdAndEstadoOrderByCreatedAtDesc(Long tiendaId, EstadoSolicitudAdmin estado);

    List<TiendaSolicitudAdmin> findByTienda_IdOrderByCreatedAtDesc(Long tiendaId);

    Optional<TiendaSolicitudAdmin> findByIdAndTienda_Id(Long solId, Long tiendaId);

    boolean existsBySolicitante_IdAndTienda_IdAndEstado(Long solicitanteId, Long tiendaId, EstadoSolicitudAdmin estado);

    // Buzón: pendientes de varias tiendas a la vez
    List<TiendaSolicitudAdmin> findByTienda_IdInAndEstadoOrderByCreatedAtDesc(Collection<Long> tiendaIds, EstadoSolicitudAdmin estado);

    // Buzón del admin de plataforma: tiendas que todavía no tienen ningún admin
    @Query("""
        select s from TiendaSolicitudAdmin s
        where s.estado = :estado
          and not exists (
              select tu.id from TiendaUsuario tu
              where tu.tienda = s.tienda
                and tu.rol = :rol
                and tu.estado = :activo
          )
        order by s.createdAt desc
    """)
    List<TiendaSolicitudAdmin> pendientesDeTiendasSinAdmin(
            @Param("estado") EstadoSolicitudAdmin estado,
            @Param("rol") RolTienda rol,
            @Param("activo") EstadoMembresia activo
    );
}
