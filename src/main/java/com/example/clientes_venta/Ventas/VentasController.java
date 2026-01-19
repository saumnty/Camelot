package com.example.clientes_venta.Ventas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Compra.Compra;
import com.example.clientes_venta.Tienda.EstadoMembresia;
import com.example.clientes_venta.Tienda.RolTienda;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaRepository;
import com.example.clientes_venta.Tienda.TiendaService;
import com.example.clientes_venta.Tienda.TiendaUsuarioRepository;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Controller
@RequestMapping("/ventas")
public class VentasController {

    private final VentasService ventasService;
    private final UsuarioRepo usuarioRepo;
    private final TiendaRepository tiendaRepo;
    private final TiendaService tiendaService;
    private final TiendaUsuarioRepository tiendaUsuarioRepo;

    public VentasController(
            VentasService ventasService,
            UsuarioRepo usuarioRepo,
            TiendaRepository tiendaRepo,
            TiendaService tiendaService,
            TiendaUsuarioRepository tiendaUsuarioRepo
    ) {
        this.ventasService = ventasService;
        this.usuarioRepo = usuarioRepo;
        this.tiendaRepo = tiendaRepo;
        this.tiendaService = tiendaService;
        this.tiendaUsuarioRepo = tiendaUsuarioRepo;
    }

    private Usuario getUsuario(Authentication auth) {
        if (auth == null) return null;
        return usuarioRepo.findByEmail(auth.getName()).orElse(null);
    }

    private Long resolverTiendaIdOredirigir(Usuario u, Long tiendaId, RedirectAttributes ra) {
        List<Long> adminTiendaIds = tiendaService.tiendasAdminIds(u);

        if (adminTiendaIds.isEmpty()) {
            ra.addFlashAttribute("mensajeError", "No eres ADMIN de ninguna tienda.");
            return null;
        }

        // Si no viene tiendaId:
        if (tiendaId == null) {
            // si solo admin de una, usar esa
            if (adminTiendaIds.size() == 1) return adminTiendaIds.get(0);

            // si admin de varias, por ahora usamos la primera (luego hacemos selector)
            ra.addFlashAttribute("mensajeError", "Tienes varias tiendas. Mostrando la primera. (Luego hacemos selector)");
            return adminTiendaIds.get(0);
        }

        // Si viene tiendaId, validar que sí sea admin de esa tienda
        if (!tiendaService.esAdminDeTienda(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para ver ventas de esta tienda.");
            return null;
        }

        return tiendaId;
    }

    @GetMapping
    public String listarVentas(
            @RequestParam(required = false) Long tiendaId,
            Model model,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario admin = getUsuario(auth);
        if (admin == null) return "redirect:/login";

        Long resolvedId = resolverTiendaIdOredirigir(admin, tiendaId, ra);
        if (resolvedId == null) return "redirect:/home";

        // si no venía tiendaId y resolvimos, redirigimos con tiendaId para que el sidebar/botones mantengan contexto
        if (tiendaId == null) {
            return "redirect:/ventas?tiendaId=" + resolvedId;
        }

        Tienda tienda = tiendaRepo.findById(resolvedId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/home";
        }

        List<Compra> ventas = ventasService.ventasPendientesPorTienda(resolvedId);

        Map<Long, String> tiendaClientePorVenta = new HashMap<>();
        for (Compra v : ventas) {
            String nombre = "—";
            if (v.getUsuario() != null && v.getUsuario().getId() != null) {
                nombre = tiendaUsuarioRepo
                        .nombreTiendaNormalAdminDeUsuario(v.getUsuario().getId())
                        .orElse("—");
            }
            tiendaClientePorVenta.put(v.getId(), nombre);
        }

        model.addAttribute("tienda", tienda);
        model.addAttribute("tiendaId", resolvedId);
        model.addAttribute("ventas", ventas);
        model.addAttribute("paginaActual", "ventas");
        model.addAttribute("tiendaClientePorVenta", tiendaClientePorVenta);

        return "ventas";
    }

    @PostMapping("/{id}/aprobar")
    public String aprobar(
            @PathVariable Long id,
            @RequestParam Long tiendaId,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario admin = getUsuario(auth);
        if (admin == null) return "redirect:/login";

        if (!tiendaService.esAdminDeTienda(admin, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para aprobar ventas de esta tienda.");
            return "redirect:/ventas?tiendaId=" + tiendaId;
        }

        ventasService.aprobarVenta(id);
        return "redirect:/ventas?tiendaId=" + tiendaId;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(
            @PathVariable Long id,
            @RequestParam Long tiendaId,
            @RequestParam(required = false) String motivo,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario admin = getUsuario(auth);
        if (admin == null) return "redirect:/login";

        if (!tiendaService.esAdminDeTienda(admin, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para cancelar ventas de esta tienda.");
            return "redirect:/ventas?tiendaId=" + tiendaId;
        }

        ventasService.cancelarVenta(id, motivo);
        return "redirect:/ventas?tiendaId=" + tiendaId;
    }

    @PostMapping("/{ventaId}/items/{itemId}/eliminar")
    public String eliminarItem(
            @PathVariable Long ventaId,
            @PathVariable Long itemId,
            @RequestParam Long tiendaId,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario admin = getUsuario(auth);
        if (admin == null) return "redirect:/login";

        if (!tiendaService.esAdminDeTienda(admin, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para editar ventas de esta tienda.");
            return "redirect:/ventas?tiendaId=" + tiendaId;
        }

        ventasService.eliminarItem(ventaId, itemId, auth.getName());
        return "redirect:/ventas?tiendaId=" + tiendaId;
    }

    @GetMapping("/historial")
    public String historial(
            @RequestParam(required = false) Long tiendaId,
            Model model,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario admin = getUsuario(auth);
        if (admin == null) return "redirect:/login";

        Long resolvedId = resolverTiendaIdOredirigir(admin, tiendaId, ra);
        if (resolvedId == null) return "redirect:/home";

        if (tiendaId == null) {
            return "redirect:/ventas/historial?tiendaId=" + resolvedId;
        }

        Tienda tienda = tiendaRepo.findById(resolvedId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/home";
        }

        List<Compra> ventas = ventasService.ventasPorTienda(resolvedId);

        model.addAttribute("tienda", tienda);
        model.addAttribute("tiendaId", resolvedId);
        model.addAttribute("ventas", ventas);
        model.addAttribute("paginaActual", "ventas");

        return "ventas_historial";
    }
}
