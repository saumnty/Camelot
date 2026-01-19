package com.example.clientes_venta.Camara;

import java.time.Duration;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Tienda.EstadoMembresia;
import com.example.clientes_venta.Tienda.RolTienda;
import com.example.clientes_venta.Tienda.TiendaUsuarioRepository;
import com.example.clientes_venta.Usuario.UsuarioRepo;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CamaraController {

    private final CamaraService camaraService;
    private final TiendaUsuarioRepository tiendaUsuarioRepo;
    private final UsuarioRepo usuarioRepo;

    @GetMapping("/camaras")
    public String camaras(
            Model model,
            HttpSession session,
            Authentication auth,
            @RequestParam(value = "tiendaId", required = false) Long tiendaId
    ) {

        // 1) Resolver tienda activa (URL -> sesión)
        if (tiendaId != null) session.setAttribute("tiendaId", tiendaId);

        Object tidObj = session.getAttribute("tiendaId");
        Long tiendaIdResuelta = (tidObj == null) ? null : Long.valueOf(tidObj.toString());

        if (tiendaIdResuelta == null) {
            model.addAttribute("mensajeError", "No hay tienda activa. Entra a /tiendas/{id} y luego a Cámaras.");
            return "camaras";
        }

        // 2) Validar ADMIN de esa tienda
        if (auth == null || auth.getName() == null) {
            model.addAttribute("mensajeError", "No autenticado.");
            return "camaras";
        }

        Integer userId = usuarioRepo.findByEmail(auth.getName()).orElseThrow().getId();

        boolean esAdmin = tiendaUsuarioRepo.existsByUsuarioIdAndTiendaIdAndRolAndEstado(
                userId, tiendaIdResuelta, RolTienda.ADMIN, EstadoMembresia.ACTIVO
        );

        if (!esAdmin) {
            model.addAttribute("mensajeError", "No tienes permisos para ver cámaras de esta tienda.");
            return "camaras";
        }

        // 3) Cargar data
        model.addAttribute("tiendaId", tiendaIdResuelta);
        model.addAttribute("camaras", camaraService.listarPorTienda(tiendaIdResuelta));
        model.addAttribute("camaraForm", new Camara());
        return "camaras";
    }

    @PostMapping("/camaras/guardar")
    public String guardar(
            @ModelAttribute("camaraForm") Camara camara,
            HttpSession session,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Long tiendaId = getTiendaIdFromSession(session);
        if (tiendaId == null) {
            ra.addFlashAttribute("mensajeError", "No hay tienda activa.");
            return "redirect:/camaras";
        }

        if (!esAdmin(auth, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos.");
            return "redirect:/camaras?tiendaId=" + tiendaId;
        }

        camaraService.guardar(tiendaId, camara);
        ra.addFlashAttribute("mensajeExito", "Cámara guardada.");
        return "redirect:/camaras?tiendaId=" + tiendaId;
    }

    @PostMapping("/camaras/{id}/toggle")
    public String toggle(@PathVariable Long id, HttpSession session, Authentication auth, RedirectAttributes ra) {
        Long tiendaId = getTiendaIdFromSession(session);
        if (tiendaId == null) return "redirect:/camaras";

        if (!esAdmin(auth, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos.");
            return "redirect:/camaras?tiendaId=" + tiendaId;
        }

        camaraService.toggleActivo(id);
        ra.addFlashAttribute("mensajeExito", "Estado actualizado.");
        return "redirect:/camaras?tiendaId=" + tiendaId;
    }

    @PostMapping("/camaras/{id}/eliminar")
    public String eliminar(@PathVariable Long id, HttpSession session, Authentication auth, RedirectAttributes ra) {
        Long tiendaId = getTiendaIdFromSession(session);
        if (tiendaId == null) return "redirect:/camaras";

        if (!esAdmin(auth, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos.");
            return "redirect:/camaras?tiendaId=" + tiendaId;
        }

        camaraService.eliminar(id);
        ra.addFlashAttribute("mensajeExito", "Cámara eliminada.");
        return "redirect:/camaras?tiendaId=" + tiendaId;
    }

    @PostMapping("/camaras/{id}/probar")
    public String probar(@PathVariable Long id, HttpSession session, Authentication auth, RedirectAttributes ra) {
        Long tiendaId = getTiendaIdFromSession(session);
        if (tiendaId == null) return "redirect:/camaras";

        if (!esAdmin(auth, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos.");
            return "redirect:/camaras?tiendaId=" + tiendaId;
        }

        Camara c = camaraService.obtener(id);
        String msg = camaraService.probarRtspDetalle(c.getRtspUrl(), Duration.ofSeconds(6));
        boolean ok = msg.startsWith("OK");
        ra.addFlashAttribute(ok ? "mensajeExito" : "mensajeError", msg);

        return "redirect:/camaras?tiendaId=" + tiendaId;
    }

    private Long getTiendaIdFromSession(HttpSession session) {
        Object tidObj = session.getAttribute("tiendaId");
        return (tidObj == null) ? null : Long.valueOf(tidObj.toString());
    }

    private boolean esAdmin(Authentication auth, Long tiendaId) {
        if (auth == null || auth.getName() == null) return false;

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getId())
                .orElse(null);

        if (userId == null) return false;

        return tiendaUsuarioRepo.existsByUsuarioIdAndTiendaIdAndRolAndEstado(
                userId, tiendaId, RolTienda.ADMIN, EstadoMembresia.ACTIVO
        );
    }

    @GetMapping("/camaras/nueva")
    public String nuevaCamara(@RequestParam Long tiendaId, Model model) {
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("camaraForm", new Camara());
        return "camaras_nueva";
    }

}