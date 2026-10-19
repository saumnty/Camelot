package com.example.clientes_venta.Notificacion;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.clientes_venta.Clientes.ClienteFrecuenteService;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaContexto;
import com.example.clientes_venta.Tienda.TiendaSolicitudAdminService;
import com.example.clientes_venta.Usuario.Usuario;

/**
 * Buzón / centro de notificaciones.
 * Arriba: "Por resolver" (solicitudes de dueño y de cliente frecuente que puedo aprobar aquí mismo).
 * Abajo: el historial de avisos (compras confirmadas, reembolsos, etc.).
 */
@Controller
public class NotificacionController {

    private final NotificacionService notificaciones;
    private final TiendaContexto contexto;
    private final TiendaSolicitudAdminService solicitudesAdmin;
    private final ClienteFrecuenteService frecuentes;

    public NotificacionController(NotificacionService notificaciones,
                                  TiendaContexto contexto,
                                  TiendaSolicitudAdminService solicitudesAdmin,
                                  ClienteFrecuenteService frecuentes) {
        this.notificaciones = notificaciones;
        this.contexto = contexto;
        this.solicitudesAdmin = solicitudesAdmin;
        this.frecuentes = frecuentes;
    }

    @GetMapping("/notificaciones")
    public String buzon(Authentication auth, Model model) {
        Usuario u = contexto.usuario(auth);
        if (u == null) return "redirect:/login";

        List<Long> mias = contexto.tiendasQueAdministra(u).stream().map(Tienda::getId).toList();

        model.addAttribute("solicitudesDueno", solicitudesAdmin.pendientesPara(u, mias));
        model.addAttribute("solicitudesFrecuente", frecuentes.pendientes(mias));
        model.addAttribute("notificaciones", notificaciones.recientes(u, 100));
        return "notificaciones";
    }

    /** Clic en una notificación: se marca leída y te lleva a donde corresponde. */
    @PostMapping("/notificaciones/{id}/abrir")
    public String abrir(@PathVariable Long id, Authentication auth) {
        String enlace = notificaciones.abrir(id, contexto.usuario(auth));
        // Solo rutas internas (evita redirecciones a otros sitios)
        if (enlace == null || !enlace.startsWith("/") || enlace.startsWith("//")) {
            return "redirect:/notificaciones";
        }
        return "redirect:" + enlace;
    }

    @PostMapping("/notificaciones/leer-todas")
    public String leerTodas(Authentication auth) {
        notificaciones.marcarTodas(contexto.usuario(auth));
        return "redirect:/notificaciones";
    }
}
