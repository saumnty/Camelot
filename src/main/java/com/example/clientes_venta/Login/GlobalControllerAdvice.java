package com.example.clientes_venta.Login;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.clientes_venta.Carrito.CarritoItem;
import com.example.clientes_venta.Carrito.CarritoRepository;
import com.example.clientes_venta.Notificacion.NotificacionService;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaContexto;
import com.example.clientes_venta.Usuario.Rol;
import com.example.clientes_venta.Usuario.Usuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Datos que necesita la barra lateral (fragments/sidebar.html) en TODAS las páginas.
 * Todos empiezan con "nav" para no chocar con lo que ponen los controladores.
 *
 *  navUsuario         usuario logueado (o null)
 *  navTiendasAdmin    tiendas que administra (vacío = es solo cliente)
 *  navTiendaAdmin     la tienda que administra ahora (sección "Mi tienda")
 *  navTiendaVisitada  tienda que está viendo como cliente (submenú de tienda); null en el inicio
 *  navSeccion         qué opción del menú se pinta como activa
 *  navCarritoCount    piezas en el carrito
 *  navNotifCount      notificaciones sin leer
 */
@ControllerAdvice(annotations = Controller.class)
public class GlobalControllerAdvice {

    private final TiendaContexto contexto;
    private final CarritoRepository carritoRepo;
    private final NotificacionService notificacionService;

    public GlobalControllerAdvice(TiendaContexto contexto,
                                  CarritoRepository carritoRepo,
                                  NotificacionService notificacionService) {
        this.contexto = contexto;
        this.carritoRepo = carritoRepo;
        this.notificacionService = notificacionService;
    }

    @ModelAttribute
    public void navegacion(Model model, Authentication auth, HttpServletRequest request) {
        Usuario u = contexto.usuario(auth);

        // "nombre" ya lo usaban varias plantillas
        model.addAttribute("nombre", u != null && u.getName() != null ? u.getName() : "Usuario");
        model.addAttribute("navUsuario", u);

        String path = request.getRequestURI();
        if (u == null) {
            model.addAttribute("navTiendasAdmin", List.of());
            model.addAttribute("navSeccion", seccion(path, false));
            return;
        }

        HttpSession session = request.getSession();
        Long tiendaIdParam = parseLong(request.getParameter("tiendaId"));

        // ---- Mi tienda (admin) ----
        List<Tienda> mias = contexto.tiendasQueAdministra(u);
        Long tiendaAdminId = mias.isEmpty() ? null : contexto.resolverTiendaAdmin(u, tiendaIdParam, session);
        Tienda tiendaAdmin = mias.stream()
                .filter(t -> t.getId().equals(tiendaAdminId))
                .findFirst().orElse(null);

        // ---- Tienda que visita como cliente ----
        Long visitadaId = tiendaVisitadaId(path, tiendaIdParam);
        boolean esSuTiendaAdmin = visitadaId != null && visitadaId.equals(tiendaAdminId);
        Tienda visitada = (visitadaId == null || esSuTiendaAdmin) ? null : contexto.tienda(visitadaId);

        model.addAttribute("navTiendasAdmin", mias);
        model.addAttribute("navTiendaAdmin", tiendaAdmin);
        model.addAttribute("navTiendaVisitada", visitada);
        model.addAttribute("navSeccion", seccion(path, esSuTiendaAdmin));
        model.addAttribute("navEsAdminPlataforma", u.getRol() == Rol.ADMIN);
        model.addAttribute("navCarritoCount", piezasEnCarrito(u));
        model.addAttribute("navNotifCount", notificacionService.noLeidas(u));
    }

    private int piezasEnCarrito(Usuario u) {
        return carritoRepo.findByUsuario_IdAndActivoTrue(u.getId())
                .map(c -> c.getItems().stream().mapToInt(CarritoItem::getCantidad).sum())
                .orElse(0);
    }

    /** Páginas que pertenecen a una tienda vista como cliente. */
    private static Long tiendaVisitadaId(String path, Long tiendaIdParam) {
        if (path.matches("^/tiendas/\\d+/?$")) {
            return parseLong(path.replaceAll("\\D", ""));
        }
        if (path.startsWith("/productos") || path.startsWith("/carrito")) {
            return tiendaIdParam;
        }
        return null;
    }

    private static String seccion(String path, boolean enSuTienda) {
        if (path.equals("/") || path.startsWith("/home")) return "tiendas";
        if (path.startsWith("/carrito") || path.startsWith("/checkout")) return "carrito";
        if (path.startsWith("/compras")) return "compras";
        if (path.startsWith("/notificaciones")) return "notificaciones";
        if (path.startsWith("/config/tienda")) return "config-tienda";
        if (path.startsWith("/config")) return "cuenta";
        if (path.startsWith("/productos")) return enSuTienda ? "admin-catalogo" : "tienda-catalogo";
        if (path.matches("^/tiendas/\\d+/?$")) return "tienda-inicio";
        if (path.startsWith("/ventas")) return "ventas";
        if (path.startsWith("/clientes")) return "clientes";
        if (path.startsWith("/admin/compras-proveedor")) return "proveedor";
        if (path.startsWith("/admin/compra-externa")) return "mostrador";
        if (path.startsWith("/admin/reportes")) return "reportes";
        if (path.startsWith("/camaras")) return "camaras";
        return "";
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
