package com.example.clientes_venta.Tienda;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Controller
@RequestMapping("/tiendas/{tiendaId}/solicitudes")
public class TiendaSolicitudAdminController {

    private final TiendaRepository tiendaRepo;
    private final TiendaSolicitudAdminRepository solicitudRepo;
    private final TiendaSolicitudAdminService solicitudService;
    private final UsuarioRepo usuarioRepo;

    public TiendaSolicitudAdminController(
            TiendaRepository tiendaRepo,
            TiendaSolicitudAdminRepository solicitudRepo,
            TiendaSolicitudAdminService solicitudService,
            UsuarioRepo usuarioRepo
    ) {
        this.tiendaRepo = tiendaRepo;
        this.solicitudRepo = solicitudRepo;
        this.solicitudService = solicitudService;
        this.usuarioRepo = usuarioRepo;
    }

    // ============================================================
    // 1) CLIENTE: enviar solicitud "Soy dueño"
    // POST /tiendas/{tiendaId}/solicitudes/dueno
    // ============================================================
    @PostMapping("/dueno")
    public String solicitarDueno(
            @PathVariable Long tiendaId,
            @RequestParam(required = false) String nota,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario solicitante = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        Tienda tienda = tiendaRepo.findById(tiendaId)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no existe"));

        // ya es admin?
        if (solicitudService.yaEsAdmin(tiendaId, solicitante)) {
            ra.addFlashAttribute("msg", "Ya eres ADMIN de esta tienda.");
            return "redirect:/tiendas/" + tiendaId;
        }

        // ya tiene solicitud pendiente?
        if (solicitudService.tieneSolicitudPendiente(tiendaId, solicitante)) {
            ra.addFlashAttribute("msg", "Ya tienes una solicitud pendiente.");
            return "redirect:/tiendas/" + tiendaId;
        }

        solicitudService.crearSolicitud(tienda, solicitante, nota);
        ra.addFlashAttribute("msg", "Solicitud enviada. Espera aprobación del ADMIN.");
        return "redirect:/tiendas/" + tiendaId;
    }

    // ============================================================
    // 2) ADMIN: ver panel (pendientes + historial)
    // GET /tiendas/{tiendaId}/solicitudes/admin
    // ============================================================
    @GetMapping("/admin")
    public String panelSolicitudesAdmin(
            @PathVariable Long tiendaId,
            Authentication auth,
            Model model,
            RedirectAttributes ra
    ) {
        Usuario admin = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        if (!solicitudService.yaEsAdmin(tiendaId, admin)) {
            ra.addFlashAttribute("msg", "No tienes permisos para ver este panel.");
            return "redirect:/tiendas/" + tiendaId;
        }

        Tienda tienda = tiendaRepo.findById(tiendaId)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no existe"));

        List<TiendaSolicitudAdmin> pendientes = solicitudService.listarPendientes(tiendaId);
        List<TiendaSolicitudAdmin> historial = solicitudService.listarTodas(tiendaId);

        model.addAttribute("tienda", tienda);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("historial", historial);
        return "tienda_solicitudes_admin";
    }

    // ============================================================
    // 3) ADMIN: aprobar
    // POST /tiendas/{tiendaId}/solicitudes/admin/{solicitudId}/aprobar
    // ============================================================
    @PostMapping("/admin/{solicitudId}/aprobar")
    public String aprobar(
            @PathVariable Long tiendaId,
            @PathVariable Long solicitudId,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario admin = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        if (!solicitudService.yaEsAdmin(tiendaId, admin)) {
            ra.addFlashAttribute("msg", "No tienes permisos para aprobar solicitudes.");
            return "redirect:/tiendas/" + tiendaId;
        }

        // extra seguridad: solicitud pertenece a tienda
        solicitudRepo.findByIdAndTienda_Id(solicitudId, tiendaId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no existe en esta tienda"));

        solicitudService.aprobar(tiendaId, solicitudId, admin);
        ra.addFlashAttribute("msg", "Solicitud aprobada.");
        return "redirect:/tiendas/" + tiendaId + "/solicitudes/admin";
    }

    // ============================================================
    // 4) ADMIN: rechazar
    // POST /tiendas/{tiendaId}/solicitudes/admin/{solicitudId}/rechazar
    // ============================================================
    @PostMapping("/admin/{solicitudId}/rechazar")
    public String rechazar(
            @PathVariable Long tiendaId,
            @PathVariable Long solicitudId,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario admin = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        if (!solicitudService.yaEsAdmin(tiendaId, admin)) {
            ra.addFlashAttribute("msg", "No tienes permisos para rechazar solicitudes.");
            return "redirect:/tiendas/" + tiendaId;
        }

        // extra seguridad: solicitud pertenece a tienda
        solicitudRepo.findByIdAndTienda_Id(solicitudId, tiendaId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no existe en esta tienda"));

        // OJO: tu service rechazar NO recibe motivo. Si lo quieres, lo agregamos luego.
        solicitudService.rechazar(tiendaId, solicitudId, admin);

        ra.addFlashAttribute("msg", "Solicitud rechazada.");
        return "redirect:/tiendas/" + tiendaId + "/solicitudes/admin";
    }
}