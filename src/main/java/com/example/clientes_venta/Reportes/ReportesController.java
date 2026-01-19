package com.example.clientes_venta.Reportes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Compra.CompraRepository;
import com.example.clientes_venta.Compra.EstadoCompra;
import com.example.clientes_venta.CompraExterna.CompraExternaRepository;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaRepository;
import com.example.clientes_venta.Tienda.TiendaService;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Controller
@RequestMapping("/admin/reportes")
public class ReportesController {

    private final CompraRepository compraRepo;
    private final CompraExternaRepository compraExternaRepo;
    private final TiendaRepository tiendaRepo;
    private final TiendaService tiendaService;
    private final UsuarioRepo usuarioRepo;

    public ReportesController(
            CompraRepository compraRepo,
            CompraExternaRepository compraExternaRepo,
            TiendaRepository tiendaRepo,
            TiendaService tiendaService,
            UsuarioRepo usuarioRepo
    ) {
        this.compraRepo = compraRepo;
        this.compraExternaRepo = compraExternaRepo;
        this.tiendaRepo = tiendaRepo;
        this.tiendaService = tiendaService;
        this.usuarioRepo = usuarioRepo;
    }

    private Usuario getUsuario(Authentication auth) {
        if (auth == null) return null;
        return usuarioRepo.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping("/ventas")
    public String reporteVentas(
            @RequestParam Long tiendaId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            Authentication auth,
            Model model,
            RedirectAttributes ra
    ) {
        Usuario u = getUsuario(auth);
        if (u == null) return "redirect:/login";

        if (!tiendaService.esAdminDeTienda(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para ver reportes.");
            return "redirect:/home";
        }

        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/home";
        }

        LocalDate d = (desde == null || desde.isBlank()) ? LocalDate.now().minusDays(6) : LocalDate.parse(desde);
        LocalDate h = (hasta == null || hasta.isBlank()) ? LocalDate.now() : LocalDate.parse(hasta);

        LocalDateTime desdeDT = d.atStartOfDay();
        LocalDateTime hastaDT = h.plusDays(1).atStartOfDay();

        model.addAttribute("tienda", tienda);
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);

        model.addAttribute("totalesPorDia",
                compraRepo.totalesPorDia(tiendaId, EstadoCompra.CONFIRMADA, desdeDT, hastaDT));
        model.addAttribute("topProductos",
                compraRepo.topProductos(tiendaId, EstadoCompra.CONFIRMADA, desdeDT, hastaDT));
        model.addAttribute("topCategorias",
                compraRepo.topCategorias(tiendaId, EstadoCompra.CONFIRMADA, desdeDT, hastaDT));

        return "reportes_ventas";
    }

    @GetMapping("/resumen-diario")
    public String resumenDiario(
            @RequestParam Long tiendaId,
            @RequestParam(required = false) String fecha,
            Authentication auth,
            Model model,
            RedirectAttributes ra
    ) {
        Usuario u = getUsuario(auth);
        if (u == null) return "redirect:/login";

        if (!tiendaService.esAdminDeTienda(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para ver reportes.");
            return "redirect:/home";
        }

        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/home";
        }

        LocalDate dia = (fecha == null || fecha.isBlank()) ? LocalDate.now() : LocalDate.parse(fecha);
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fin = dia.plusDays(1).atStartOfDay();

        BigDecimal ingresos = compraRepo.ingresosDelDia(tiendaId, EstadoCompra.CONFIRMADA, inicio, fin);
        BigDecimal costo = compraRepo.costoEstimadoDelDia(tiendaId, EstadoCompra.CONFIRMADA, inicio, fin);
        BigDecimal compraExterna = compraExternaRepo.totalDelDia(tiendaId, dia);

        BigDecimal utilidad = ingresos.subtract(costo);         // utilidad por costoProveedor
        BigDecimal neto = ingresos.subtract(compraExterna);     // neto por gastos externos
        BigDecimal utilidadNeta = utilidad.subtract(compraExterna); // lo “más realista”

        model.addAttribute("tienda", tienda);
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("fecha", dia);

        model.addAttribute("ingresos", ingresos);
        model.addAttribute("costoEstimado", costo);
        model.addAttribute("utilidadEstimada", utilidad);

        model.addAttribute("compraExterna", compraExterna);
        model.addAttribute("netoDelDia", neto);
        model.addAttribute("utilidadNeta", utilidadNeta);

        return "resumen_diario";
    }
}