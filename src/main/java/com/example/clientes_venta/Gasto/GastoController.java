package com.example.clientes_venta.Gasto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Tienda.*;
import com.example.clientes_venta.Usuario.*;

@Controller
@RequestMapping("/admin/compras-proveedor")
public class GastoController {

    private final GastoRepository gastoRepo;
    private final TiendaRepository tiendaRepo;
    private final TiendaService tiendaService;
    private final UsuarioRepo usuarioRepo;

    public GastoController(GastoRepository gastoRepo, TiendaRepository tiendaRepo, TiendaService tiendaService, UsuarioRepo usuarioRepo) {
        this.gastoRepo = gastoRepo;
        this.tiendaRepo = tiendaRepo;
        this.tiendaService = tiendaService;
        this.usuarioRepo = usuarioRepo;
    }

    private Usuario getUsuario(Authentication auth) {
        if (auth == null) return null;
        return usuarioRepo.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping
    public String ver(@RequestParam Long tiendaId,
                      @RequestParam(required = false) String fecha,
                      Model model,
                      Authentication auth,
                      RedirectAttributes ra) {

        Usuario u = getUsuario(auth);

        if (!tiendaService.esAdminDeTienda(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para ver compras de proveedor.");
            return "redirect:/productos?tiendaId=" + tiendaId;
        }

        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/productos";
        }

        LocalDate dia = (fecha == null || fecha.isBlank()) ? LocalDate.now() : LocalDate.parse(fecha);

        List<Gasto> gastos = gastoRepo.findByTienda_IdAndFechaOrderByCreatedAtDesc(tiendaId, dia);
        List<GastoRepository.TotalProveedorRow> resumen = gastoRepo.totalesPorProveedor(tiendaId, dia);
        BigDecimal totalDia = gastoRepo.totalGastosDelDia(tiendaId, dia);

        model.addAttribute("tienda", tienda);
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("fecha", dia);
        model.addAttribute("gastos", gastos);
        model.addAttribute("resumenProveedor", resumen);
        model.addAttribute("totalGastos", totalDia);
        model.addAttribute("tipos", TipoGasto.values());

        return "admin_compras_proveedor";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam Long tiendaId,
                          @RequestParam String proveedor,
                          @RequestParam TipoGasto tipo,
                          @RequestParam BigDecimal total,
                          @RequestParam(required = false) String nota,
                          @RequestParam(required = false) String fecha,
                          Authentication auth,
                          RedirectAttributes ra) {

        Usuario u = getUsuario(auth);

        if (!tiendaService.esAdminDeTienda(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para registrar gastos.");
            return "redirect:/productos?tiendaId=" + tiendaId;
        }

        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/productos";
        }

        String prov = (proveedor == null) ? "" : proveedor.trim();
        if (prov.isBlank()) {
            ra.addFlashAttribute("mensajeError", "El proveedor no puede ir vacío.");
            return "redirect:/admin/compras-proveedor?tiendaId=" + tiendaId;
        }
        if (prov.length() > 120) prov = prov.substring(0, 120);

        if (total == null || total.signum() < 0) {
            ra.addFlashAttribute("mensajeError", "El total debe ser mayor o igual a 0.");
            return "redirect:/admin/compras-proveedor?tiendaId=" + tiendaId;
        }

        LocalDate dia = (fecha == null || fecha.isBlank()) ? LocalDate.now() : LocalDate.parse(fecha);

        Gasto g = Gasto.builder()
                .tienda(tienda)
                .createdBy(u)
                .createdAt(LocalDateTime.now())
                .fecha(dia)
                .tipo(tipo)
                .proveedor(prov)
                .total(total)
                .nota((nota != null && !nota.isBlank()) ? nota.trim() : null)
                .build();

        gastoRepo.save(g);

        ra.addFlashAttribute("mensajeExito", "Gasto registrado.");
        return "redirect:/admin/compras-proveedor?tiendaId=" + tiendaId + "&fecha=" + dia;
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id,
                           @RequestParam Long tiendaId,
                           @RequestParam(required = false) String fecha,
                           Authentication auth,
                           RedirectAttributes ra) {

        Usuario u = getUsuario(auth);

        if (!tiendaService.esAdminDeTienda(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos.");
            return "redirect:/productos?tiendaId=" + tiendaId;
        }

        Gasto g = gastoRepo.findById(id).orElse(null);
        if (g != null && g.getTienda() != null && g.getTienda().getId().equals(tiendaId)) {
            gastoRepo.deleteById(id);
            ra.addFlashAttribute("mensajeExito", "Gasto eliminado.");
        }

        String dia = (fecha == null || fecha.isBlank()) ? LocalDate.now().toString() : fecha;
        return "redirect:/admin/compras-proveedor?tiendaId=" + tiendaId + "&fecha=" + dia;
    }
}