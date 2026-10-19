package com.example.clientes_venta.Notificacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByDestinatario_IdOrderByCreatedAtDesc(Integer usuarioId, Pageable pageable);

    long countByDestinatario_IdAndLeidaFalse(Integer usuarioId);

    Optional<Notificacion> findByIdAndDestinatario_Id(Long id, Integer usuarioId);

    @Modifying
    @Query("update Notificacion n set n.leida = true where n.destinatario.id = :usuarioId and n.leida = false")
    int marcarTodasLeidas(@Param("usuarioId") Integer usuarioId);

    // Cuando un admin resuelve una solicitud, los avisos de los demás admins ya no son pendientes
    @Modifying
    @Query("update Notificacion n set n.leida = true where n.tipo = :tipo and n.referenciaId = :referenciaId")
    int marcarLeidasPorReferencia(@Param("tipo") TipoNotificacion tipo, @Param("referenciaId") Long referenciaId);
}
