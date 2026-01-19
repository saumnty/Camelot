package com.example.clientes_venta.Tienda;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Usuario.Usuario;

@Service
public class TiendaSolicitudAdminService {

    private final TiendaSolicitudAdminRepository solicitudRepo;
    private final TiendaUsuarioRepository tiendaUsuarioRepo;

    public TiendaSolicitudAdminService(TiendaSolicitudAdminRepository solicitudRepo,
                                      TiendaUsuarioRepository tiendaUsuarioRepo) {
        this.solicitudRepo = solicitudRepo;
        this.tiendaUsuarioRepo = tiendaUsuarioRepo;
    }

    public boolean yaEsAdmin(Long tiendaId, Usuario u) {
        if (u == null) return false;
        return tiendaUsuarioRepo.existsByTienda_IdAndUsuario_IdAndRolAndEstado(
            tiendaId,
            u.getId(),
            RolTienda.ADMIN,
            EstadoMembresia.ACTIVO
        );
    }

    public boolean tieneSolicitudPendiente(Long tiendaId, Usuario u) {
        if (u == null) return false;
        return solicitudRepo.existsByTienda_IdAndSolicitante_IdAndEstado(tiendaId, u.getId(), EstadoSolicitudAdmin.PENDIENTE);
    }

    @Transactional
    public void crearSolicitud(Tienda tienda, Usuario solicitante, String notaOpcional) {

        // si ya es admin → nada
        if (yaEsAdmin(tienda.getId(), solicitante)) return;

        // si ya existe pendiente → nada
        boolean existePendiente = solicitudRepo.existsByTienda_IdAndSolicitante_IdAndEstado(
                tienda.getId(), solicitante.getId(), EstadoSolicitudAdmin.PENDIENTE
        );
        if (existePendiente) return;

        // si ya existe una solicitud APROBADA (histórica) → bloquear nuevas
        boolean yaAprobada = solicitudRepo.existsByTienda_IdAndSolicitante_IdAndEstado(
                tienda.getId(), solicitante.getId(), EstadoSolicitudAdmin.APROBADA
        );
        if (yaAprobada) return;

        TiendaSolicitudAdmin s = new TiendaSolicitudAdmin();
        s.setTienda(tienda);
        s.setSolicitante(solicitante);
        s.setEstado(EstadoSolicitudAdmin.PENDIENTE);
        s.setCreatedAt(LocalDateTime.now());
        s.setNota(notaOpcional);

        solicitudRepo.save(s);
    }

    public List<TiendaSolicitudAdmin> listarPendientes(Long tiendaId) {
        return solicitudRepo.findByTienda_IdAndEstadoOrderByCreatedAtDesc(tiendaId, EstadoSolicitudAdmin.PENDIENTE);
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

        // Upsert TiendaUsuario -> ADMIN
        upsertAdminMembership(tiendaId, s.getSolicitante());
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
    }

    private void upsertAdminMembership(Long tiendaId, Usuario user) {
        TiendaUsuario tu = tiendaUsuarioRepo.findByTienda_IdAndUsuario_Id(tiendaId, user.getId())
                .orElseGet(TiendaUsuario::new);

        // Si tu TiendaUsuario requiere setTienda y setUsuario:
        // tu.setTienda(new Tienda(tiendaId)) NO recomendado si no tienes constructor.
        // Mejor: si tu entity TiendaUsuario usa relaciones, aquí debes setearlas con objetos reales.
        //
        // Te doy dos opciones:
        // Opción A (si TiendaUsuario tiene tienda y usuario como entidades):
        // tu.setTienda(tienda); tu.setUsuario(user);
        //
        // Opción B (si TiendaUsuario tiene tiendaId y usuarioId):
        // tu.setTiendaId(tiendaId); tu.setUsuarioId(user.getId());
        //
        // Ajusta según tu modelo.

        tu.setUsuario(user);
        // necesitas cargar Tienda para setearla, o setear por referencia:
        Tienda t = new Tienda();
        t.setId(tiendaId);
        tu.setTienda(t);

        tu.setRol(RolTienda.ADMIN); // ajusta si es enum
        tiendaUsuarioRepo.save(tu);
    }

    public boolean solicitudPendiente(Long usuarioId, Long tiendaId) {
        return solicitudRepo.existsBySolicitante_IdAndTienda_IdAndEstado(usuarioId, tiendaId, EstadoSolicitudAdmin.PENDIENTE);
    }

    public List<TiendaSolicitudAdmin> listarTodas(Long tiendaId) {
        return solicitudRepo.findByTienda_IdOrderByCreatedAtDesc(tiendaId);
    }

}