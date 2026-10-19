package com.example.clientes_venta.Tienda;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

import jakarta.servlet.http.HttpSession;

/**
 * Resuelve "qué tienda administro ahora mismo".
 *
 * Antes se guardaba en sesión la última tienda VISITADA ("tiendaId") y Configuración/Cámaras
 * la usaban: por eso un cliente que entraba a "Abarrotes Mendoza" veía la configuración
 * de esa tienda. Ahora hay dos ideas separadas:
 *   - tienda visitada: la que estoy viendo como cliente (solo vive en la URL).
 *   - tienda administrada: una de MIS tiendas (sesión "tiendaAdminId"), validada siempre.
 */
@Service
public class TiendaContexto {

    public static final String SESION_TIENDA_ADMIN = "tiendaAdminId";

    private final UsuarioRepo usuarioRepo;
    private final TiendaRepository tiendaRepo;
    private final TiendaUsuarioRepository tiendaUsuarioRepo;

    public TiendaContexto(UsuarioRepo usuarioRepo,
                          TiendaRepository tiendaRepo,
                          TiendaUsuarioRepository tiendaUsuarioRepo) {
        this.usuarioRepo = usuarioRepo;
        this.tiendaRepo = tiendaRepo;
        this.tiendaUsuarioRepo = tiendaUsuarioRepo;
    }

    public Usuario usuario(Authentication auth) {
        if (auth == null || auth.getName() == null || !auth.isAuthenticated()) return null;
        return usuarioRepo.findByEmail(auth.getName()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Tienda> tiendasQueAdministra(Usuario u) {
        if (u == null || u.getId() == null) return List.of();
        return tiendaUsuarioRepo.findTiendasByUsuarioRolEstado(u.getId(), RolTienda.ADMIN, EstadoMembresia.ACTIVO);
    }

    @Transactional(readOnly = true)
    public boolean esAdmin(Usuario u, Long tiendaId) {
        if (u == null || u.getId() == null || tiendaId == null) return false;
        return tiendaUsuarioRepo.existsByTienda_IdAndUsuario_IdAndRolAndEstado(
                tiendaId, u.getId(), RolTienda.ADMIN, EstadoMembresia.ACTIVO);
    }

    /**
     * Devuelve el id de la tienda que el usuario administra en este momento, o null si no administra ninguna.
     * Prioridad: tiendaId de la URL (si es suya) → la guardada en sesión (si sigue siendo suya) → la primera NORMAL.
     */
    public Long resolverTiendaAdmin(Usuario u, Long tiendaIdParam, HttpSession session) {
        if (u == null) return null;

        if (tiendaIdParam != null && esAdmin(u, tiendaIdParam)) {
            session.setAttribute(SESION_TIENDA_ADMIN, tiendaIdParam);
            return tiendaIdParam;
        }

        Object enSesion = session.getAttribute(SESION_TIENDA_ADMIN);
        if (enSesion != null) {
            try {
                Long id = Long.valueOf(enSesion.toString());
                if (esAdmin(u, id)) return id;
            } catch (NumberFormatException ignored) {
                // valor viejo o corrupto: lo ignoramos
            }
            session.removeAttribute(SESION_TIENDA_ADMIN);
        }

        List<Tienda> mias = tiendasQueAdministra(u);
        if (mias.isEmpty()) return null;

        Long id = mias.stream()
                .filter(t -> t.getTipo() == TipoTienda.NORMAL)
                .map(Tienda::getId)
                .findFirst()
                .orElse(mias.get(0).getId());
        session.setAttribute(SESION_TIENDA_ADMIN, id);
        return id;
    }

    public Tienda tienda(Long id) {
        return id == null ? null : tiendaRepo.findById(id).orElse(null);
    }
}
