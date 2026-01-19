package com.example.clientes_venta.CompraExterna;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaRepository;
import com.example.clientes_venta.Tienda.TiendaService;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Controller
@RequestMapping("/admin/compra-externa")
public class CompraExternaController {

    private final CompraExternaRepository compraExternaRepo;
    private final TiendaRepository tiendaRepo;
    private final TiendaService tiendaService;
    private final UsuarioRepo usuarioRepo;

    public CompraExternaController(
            CompraExternaRepository compraExternaRepo,
            TiendaRepository tiendaRepo,
            TiendaService tiendaService,
            UsuarioRepo usuarioRepo
    ) {
        this.compraExternaRepo = compraExternaRepo;
        this.tiendaRepo = tiendaRepo;
        this.tiendaService = tiendaService;
        this.usuarioRepo = usuarioRepo;
    }

    private Usuario getUsuario(Authentication auth) {
        if (auth == null) return null;
        return usuarioRepo.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping
    public String ver(
            @RequestParam Long tiendaId,
            @RequestParam(required = false) String fecha,
            Model model,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario u = getUsuario(auth);
        if (u == null) return "redirect:/login";

        if (!tiendaService.esAdminDeTienda(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para registrar compra externa.");
            return "redirect:/home";
        }

        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/home";
        }

        LocalDate dia = (fecha == null || fecha.isBlank()) ? LocalDate.now() : LocalDate.parse(fecha);

        List<CompraExterna> lista = compraExternaRepo.findByTienda_IdAndFechaOrderByCreatedAtDesc(tiendaId, dia);
        BigDecimal totalDia = compraExternaRepo.totalDelDia(tiendaId, dia);

        model.addAttribute("tienda", tienda);
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("fecha", dia);
        model.addAttribute("lista", lista);
        model.addAttribute("totalDia", totalDia);
        model.addAttribute("paginaActual", "compras");

        return "compra_externa";
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam Long tiendaId,
            @RequestParam String concepto,
            @RequestParam BigDecimal total,
            @RequestParam(required = false) String nota,
            @RequestParam(required = false) String fecha,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario u = getUsuario(auth);
        if (u == null) return "redirect:/login";

        if (!tiendaService.esAdminDeTienda(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos.");
            return "redirect:/home";
        }

        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/home";
        }

        String c = (concepto == null) ? "" : concepto.trim();
        if (c.isBlank()) {
            ra.addFlashAttribute("mensajeError", "El concepto no puede ir vacío.");
            return "redirect:/admin/compra-externa?tiendaId=" + tiendaId;
        }
        if (c.length() > 140) c = c.substring(0, 140);

        if (total == null || total.signum() < 0) {
            ra.addFlashAttribute("mensajeError", "El total debe ser mayor o igual a 0.");
            return "redirect:/admin/compra-externa?tiendaId=" + tiendaId;
        }

        LocalDate dia = (fecha == null || fecha.isBlank()) ? LocalDate.now() : LocalDate.parse(fecha);

        CompraExterna ce = CompraExterna.builder()
                .tienda(tienda)
                .createdBy(u)
                .createdAt(LocalDateTime.now())
                .fecha(dia)
                .concepto(c)
                .total(total)
                .nota((nota != null && !nota.isBlank()) ? nota.trim() : null)
                .build();

        compraExternaRepo.save(ce);

        ra.addFlashAttribute("mensajeExito", "Compra externa registrada.");
        return "redirect:/admin/compra-externa?tiendaId=" + tiendaId + "&fecha=" + dia;
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id,
            @RequestParam Long tiendaId,
            @RequestParam(required = false) String fecha,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario u = getUsuario(auth);
        if (u == null) return "redirect:/login";

        if (!tiendaService.esAdminDeTienda(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos.");
            return "redirect:/home";
        }

        CompraExterna ce = compraExternaRepo.findById(id).orElse(null);
        if (ce != null && ce.getTienda() != null && ce.getTienda().getId().equals(tiendaId)) {
            compraExternaRepo.deleteById(id);
            ra.addFlashAttribute("mensajeExito", "Eliminado.");
        }

        String dia = (fecha == null || fecha.isBlank()) ? LocalDate.now().toString() : fecha;
        return "redirect:/admin/compra-externa?tiendaId=" + tiendaId + "&fecha=" + dia;
    }

    @GetMapping("/ticket/{id}")
    public String verTicketExterno(@PathVariable Long id,
                                @RequestParam Long tiendaId,
                                Authentication auth,
                                Model model,
                                RedirectAttributes ra) {

        Usuario u = getUsuario(auth);
        if (u == null) return "redirect:/login";

        if (!tiendaService.esAdminDeTienda(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos.");
            return "redirect:/home";
        }

        CompraExterna ce = compraExternaRepo.findById(id).orElse(null);
        if (ce == null || ce.getTienda() == null || !ce.getTienda().getId().equals(tiendaId)) {
            ra.addFlashAttribute("mensajeError", "Ticket externo no existe.");
            return "redirect:/compras";
        }

        model.addAttribute("externa", ce);
        model.addAttribute("paginaActual", "compras");
        return "ticket_externo";
    }
}