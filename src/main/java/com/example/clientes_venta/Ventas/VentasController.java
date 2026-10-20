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
import com.example.clientes_venta.Configuracion.ConfiguracionTiendaService;
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
    private final ConfiguracionTiendaService configService;

    public VentasController(
            VentasService ventasService,
            UsuarioRepo usuarioRepo,
            TiendaRepository tiendaRepo,
            TiendaService tiendaService,
            TiendaUsuarioRepository tiendaUsuarioRepo,
            ConfiguracionTiendaService configService
    ) {
        this.ventasService = ventasService;
        this.usuarioRepo = usuarioRepo;
        this.tiendaRepo = tiendaRepo;
        this.tiendaService = tiendaService;
        this.tiendaUsuarioRepo = tiendaUsuarioRepo;
        this.configService = configService;
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

    // ===================== Acciones sobre un pedido =====================
    // Todas: validan admin de la tienda, llaman al servicio y muestran el error si algo no se puede.

    private interface Accion { void run(Usuario admin); }

    /** volver=buzon → regresa a Notificaciones en vez de a Ventas. */
    private static String destino(String volver, String porDefecto) {
        return "buzon".equals(volver) ? "/notificaciones" : porDefecto;
    }

    private String ejecutar(Long tiendaId, String volverA, Authentication auth, RedirectAttributes ra,
                            String exito, Accion accion) {
        Usuario admin = getUsuario(auth);
        if (admin == null) return "redirect:/login";
        if (!tiendaService.esAdminDeTienda(admin, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos sobre las ventas de esta tienda.");
            return "redirect:/home";
        }
        try {
            accion.run(admin);
            ra.addFlashAttribute("mensajeExito", exito);
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:" + volverA + "?tiendaId=" + tiendaId;
    }

    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable Long id, @RequestParam Long tiendaId,
                          @RequestParam(required = false) String volver,
                          Authentication auth, RedirectAttributes ra) {
        return ejecutar(tiendaId, destino(volver, "/ventas"), auth, ra, "Pedido #" + id + " confirmado.",
                admin -> ventasService.aprobarVenta(id, tiendaId, admin));
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, @RequestParam Long tiendaId,
                           @RequestParam(required = false) String motivo,
                           @RequestParam(required = false) String volver,
                           Authentication auth, RedirectAttributes ra) {
        return ejecutar(tiendaId, destino(volver, "/ventas"), auth, ra, "Pedido #" + id + " cancelado.",
                admin -> ventasService.cancelarVenta(id, tiendaId, motivo));
    }

    @PostMapping("/{id}/reembolsar")
    public String reembolsar(@PathVariable Long id, @RequestParam Long tiendaId,
                             @RequestParam(required = false) String motivo,
                             Authentication auth, RedirectAttributes ra) {
        return ejecutar(tiendaId, "/ventas/historial", auth, ra, "Pedido #" + id + " reembolsado.",
                admin -> ventasService.reembolsarVenta(id, tiendaId, motivo, admin));
    }

    @PostMapping("/{id}/cobrado")
    public String cobrado(@PathVariable Long id, @RequestParam Long tiendaId,
                          Authentication auth, RedirectAttributes ra) {
        return ejecutar(tiendaId, "/ventas/historial", auth, ra, "Cobro en efectivo registrado.",
                admin -> ventasService.marcarCobrado(id, tiendaId));
    }

    @PostMapping("/{ventaId}/items/{itemId}/eliminar")
    public String eliminarItem(@PathVariable Long ventaId, @PathVariable Long itemId,
                               @RequestParam Long tiendaId,
                               Authentication auth, RedirectAttributes ra) {
        return ejecutar(tiendaId, "/ventas", auth, ra, "Producto quitado del pedido.",
                admin -> ventasService.eliminarItem(ventaId, itemId, tiendaId));
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
        model.addAttribute("permiteReembolsos", configService.de(resolvedId).isPermiteReembolsos());

        return "ventas_historial";
    }
}
