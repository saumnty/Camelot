package com.example.clientes_venta.Tienda;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Notificacion.NotificacionService;
import com.example.clientes_venta.Notificacion.TipoNotificacion;
import com.example.clientes_venta.Usuario.Rol;
import com.example.clientes_venta.Usuario.Usuario;

/**
 * Solicitudes "Soy dueño de esta tienda".
 * Llegan al buzón (/notificaciones) de los admins de la tienda; si la tienda todavía
 * no tiene admins, llegan a los administradores de la plataforma (Rol.ADMIN).
 */
@Service
public class TiendaSolicitudAdminService {

    private final TiendaSolicitudAdminRepository solicitudRepo;
    private final TiendaUsuarioRepository tiendaUsuarioRepo;
    private final NotificacionService notificaciones;

    public TiendaSolicitudAdminService(TiendaSolicitudAdminRepository solicitudRepo,
                                       TiendaUsuarioRepository tiendaUsuarioRepo,
                                       NotificacionService notificaciones) {
        this.solicitudRepo = solicitudRepo;
        this.tiendaUsuarioRepo = tiendaUsuarioRepo;
        this.notificaciones = notificaciones;
    }

    public boolean yaEsAdmin(Long tiendaId, Usuario u) {
        if (u == null) return false;
        return tiendaUsuarioRepo.existsByTienda_IdAndUsuario_IdAndRolAndEstado(
                tiendaId, u.getId(), RolTienda.ADMIN, EstadoMembresia.ACTIVO);
    }

    public boolean tieneSolicitudPendiente(Long tiendaId, Usuario u) {
        if (u == null) return false;
        return solicitudRepo.existsByTienda_IdAndSolicitante_IdAndEstado(tiendaId, u.getId(), EstadoSolicitudAdmin.PENDIENTE);
    }

    public boolean tiendaTieneAdmins(Long tiendaId) {
        return !tiendaUsuarioRepo.findUsuariosByTiendaRolEstado(tiendaId, RolTienda.ADMIN, EstadoMembresia.ACTIVO).isEmpty();
    }

    /** Quién puede aprobar/rechazar: un admin de la tienda, o el admin de plataforma si la tienda no tiene admins. */
    public boolean puedeResolver(Long tiendaId, Usuario u) {
        if (u == null) return false;
        if (yaEsAdmin(tiendaId, u)) return true;
        return u.getRol() == Rol.ADMIN && !tiendaTieneAdmins(tiendaId);
    }

    @Transactional
    public void crearSolicitud(Tienda tienda, Usuario solicitante, String notaOpcional) {
        if (yaEsAdmin(tienda.getId(), solicitante)) return;
        if (tieneSolicitudPendiente(tienda.getId(), solicitante)) return;

        TiendaSolicitudAdmin s = new TiendaSolicitudAdmin();
        s.setTienda(tienda);
        s.setSolicitante(solicitante);
        s.setEstado(EstadoSolicitudAdmin.PENDIENTE);
        s.setCreatedAt(LocalDateTime.now());
        String nota = (notaOpcional == null || notaOpcional.isBlank()) ? null : notaOpcional.trim();
        if (nota != null && nota.length() > 300) nota = nota.substring(0, 300);
        s.setNota(nota);
        solicitudRepo.save(s);

        notificaciones.notificarAdmins(tienda, TipoNotificacion.SOLICITUD_ADMIN,
                solicitante.getName() + " dice ser dueño de " + tienda.getNombre(),
                nota == null ? "Revisa la solicitud en tu buzón." : "Nota: " + nota,
                "/notificaciones", s.getId());
    }

    public List<TiendaSolicitudAdmin> listarPendientes(Long tiendaId) {
        return solicitudRepo.findByTienda_IdAndEstadoOrderByCreatedAtDesc(tiendaId, EstadoSolicitudAdmin.PENDIENTE);
    }

    public List<TiendaSolicitudAdmin> listarTodas(Long tiendaId) {
        return solicitudRepo.findByTienda_IdOrderByCreatedAtDesc(tiendaId);
    }

    /** Pendientes que este usuario puede resolver (para el buzón). */
    public List<TiendaSolicitudAdmin> pendientesPara(Usuario u, Collection<Long> tiendasQueAdministra) {
        Map<Long, TiendaSolicitudAdmin> unicas = new LinkedHashMap<>();
        if (tiendasQueAdministra != null && !tiendasQueAdministra.isEmpty()) {
            solicitudRepo.findByTienda_IdInAndEstadoOrderByCreatedAtDesc(tiendasQueAdministra, EstadoSolicitudAdmin.PENDIENTE)
                    .forEach(s -> unicas.put(s.getId(), s));
        }
        if (u != null && u.getRol() == Rol.ADMIN) {
            solicitudRepo.pendientesDeTiendasSinAdmin(EstadoSolicitudAdmin.PENDIENTE, RolTienda.ADMIN, EstadoMembresia.ACTIVO)
                    .forEach(s -> unicas.put(s.getId(), s));
        }
        return new ArrayList<>(unicas.values());
    }

    @Transactional
    public void aprobar(Long tiendaId, Long solicitudId, Usuario adminAprobador) {
        TiendaSolicitudAdmin s = solicitudRepo.findByIdAndTienda_Id(solicitudId, tiendaId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (s.getEstado() != EstadoSolicitudAdmin.PENDIENTE) return;

        s.setEstado(EstadoSolicitudAdmin.APROBADA);
        s.setResolvedAt(LocalDateTime.now());
        s.setResolvedBy(adminAprobador);
        solicitudRepo.save(s);

        // Alta (o reactivación) como ADMIN de la tienda
        TiendaUsuario tu = tiendaUsuarioRepo.findByTienda_IdAndUsuario_Id(tiendaId, s.getSolicitante().getId())
                .orElseGet(TiendaUsuario::new);
        tu.setTienda(s.getTienda());
        tu.setUsuario(s.getSolicitante());
        tu.setRol(RolTienda.ADMIN);
        tu.setEstado(EstadoMembresia.ACTIVO);
        tiendaUsuarioRepo.save(tu);

        notificaciones.resolverReferencia(TipoNotificacion.SOLICITUD_ADMIN, s.getId());
        notificaciones.notificar(s.getSolicitante(), s.getTienda(), TipoNotificacion.SOLICITUD_ADMIN_RESUELTA,
                "Ya eres administrador de " + s.getTienda().getNombre(),
                "En la barra lateral aparece la sección \"Mi tienda\".",
                "/productos?tiendaId=" + tiendaId, s.getId());
    }

    @Transactional
    public void rechazar(Long tiendaId, Long solicitudId, Usuario adminAprobador) {
        TiendaSolicitudAdmin s = solicitudRepo.findByIdAndTienda_Id(solicitudId, tiendaId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (s.getEstado() != EstadoSolicitudAdmin.PENDIENTE) return;

        s.setEstado(EstadoSolicitudAdmin.RECHAZADA);
        s.setResolvedAt(LocalDateTime.now());
        s.setResolvedBy(adminAprobador);
        solicitudRepo.save(s);

        notificaciones.resolverReferencia(TipoNotificacion.SOLICITUD_ADMIN, s.getId());
        notificaciones.notificar(s.getSolicitante(), s.getTienda(), TipoNotificacion.SOLICITUD_ADMIN_RESUELTA,
                "Tu solicitud de dueño en " + s.getTienda().getNombre() + " fue rechazada",
                null, "/tiendas/" + tiendaId, s.getId());
    }
}
