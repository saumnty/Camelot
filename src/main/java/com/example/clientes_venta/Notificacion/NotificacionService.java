package com.example.clientes_venta.Notificacion;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Tienda.EstadoMembresia;
import com.example.clientes_venta.Tienda.RolTienda;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaUsuarioRepository;
import com.example.clientes_venta.Usuario.Rol;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

/**
 * Buzón de cada usuario. Las demás partes del sistema solo llaman a notificar(...) o
 * notificarAdmins(...); la página /notificaciones se encarga de mostrarlas.
 */
@Service
public class NotificacionService {

    private final NotificacionRepository repo;
    private final TiendaUsuarioRepository tiendaUsuarioRepo;
    private final UsuarioRepo usuarioRepo;

    public NotificacionService(NotificacionRepository repo,
                               TiendaUsuarioRepository tiendaUsuarioRepo,
                               UsuarioRepo usuarioRepo) {
        this.repo = repo;
        this.tiendaUsuarioRepo = tiendaUsuarioRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Transactional
    public void notificar(Usuario destinatario, Tienda tienda, TipoNotificacion tipo,
                          String titulo, String mensaje, String enlace, Long referenciaId) {
        if (destinatario == null) return;
        Notificacion n = new Notificacion();
        n.setDestinatario(destinatario);
        n.setTienda(tienda);
        n.setTipo(tipo);
        n.setTitulo(recortar(titulo, 160));
        n.setMensaje(recortar(mensaje, 500));
        n.setEnlace(recortar(enlace, 255));
        n.setReferenciaId(referenciaId);
        repo.save(n);
    }

    /**
     * Avisa a todos los admins de la tienda. Si la tienda aún no tiene admins
     * (por ejemplo, la primera solicitud "soy dueño"), avisa a los admins de la plataforma.
     */
    @Transactional
    public void notificarAdmins(Tienda tienda, TipoNotificacion tipo,
                                String titulo, String mensaje, String enlace, Long referenciaId) {
        for (Usuario admin : adminsDe(tienda)) {
            notificar(admin, tienda, tipo, titulo, mensaje, enlace, referenciaId);
        }
    }

    @Transactional(readOnly = true)
    public Set<Usuario> adminsDe(Tienda tienda) {
        Set<Usuario> destinatarios = new LinkedHashSet<>();
        if (tienda != null && tienda.getId() != null) {
            destinatarios.addAll(tiendaUsuarioRepo.findUsuariosByTiendaRolEstado(
                    tienda.getId(), RolTienda.ADMIN, EstadoMembresia.ACTIVO));
        }
        if (destinatarios.isEmpty()) {
            destinatarios.addAll(usuarioRepo.findByRol(Rol.ADMIN));
        }
        return destinatarios;
    }

    @Transactional(readOnly = true)
    public List<Notificacion> recientes(Usuario u, int max) {
        if (u == null) return List.of();
        return repo.findByDestinatario_IdOrderByCreatedAtDesc(u.getId(), PageRequest.of(0, max));
    }

    @Transactional(readOnly = true)
    public long noLeidas(Usuario u) {
        if (u == null) return 0;
        return repo.countByDestinatario_IdAndLeidaFalse(u.getId());
    }

    /** Marca como leída y devuelve el enlace al que hay que ir (o null). */
    @Transactional
    public String abrir(Long id, Usuario u) {
        if (u == null) return null;
        return repo.findByIdAndDestinatario_Id(id, u.getId())
                .map(n -> {
                    n.setLeida(true);
                    return n.getEnlace();
                })
                .orElse(null);
    }

    @Transactional
    public void marcarTodas(Usuario u) {
        if (u != null) repo.marcarTodasLeidas(u.getId());
    }

    @Transactional
    public void resolverReferencia(TipoNotificacion tipo, Long referenciaId) {
        if (referenciaId != null) repo.marcarLeidasPorReferencia(tipo, referenciaId);
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
