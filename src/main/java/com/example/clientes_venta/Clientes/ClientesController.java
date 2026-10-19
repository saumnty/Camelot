package com.example.clientes_venta.Clientes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Compra.CompraRepository;
import com.example.clientes_venta.Compra.EstadoCompra;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaContexto;
import com.example.clientes_venta.Usuario.Usuario;

import jakarta.servlet.http.HttpSession;

/**
 * Clientes de MI tienda (solo admins). Tres pestañas:
 *   compradores → usuarios que ya compraron (sale solo, de las compras confirmadas)
 *   frecuentes  → clientes frecuentes y su cuenta de fiado
 *   libreta     → clientes sin cuenta en Camelot (la libreta manual de antes, ahora por tienda)
 */
@Controller
public class ClientesController {

    private final TiendaContexto contexto;
    private final ClienteRepository libretaRepo;
    private final ClienteFrecuenteService frecuentes;
    private final CompraRepository compraRepo;

    public ClientesController(TiendaContexto contexto,
                              ClienteRepository libretaRepo,
                              ClienteFrecuenteService frecuentes,
                              CompraRepository compraRepo) {
        this.contexto = contexto;
        this.libretaRepo = libretaRepo;
        this.frecuentes = frecuentes;
        this.compraRepo = compraRepo;
    }

    /** Devuelve la tienda si el usuario es admin de ella; si no, null. */
    private Tienda tiendaAdmin(Authentication auth, Long tiendaId, HttpSession session) {
        Usuario u = contexto.usuario(auth);
        Long id = contexto.resolverTiendaAdmin(u, tiendaId, session);
        if (id == null || (tiendaId != null && !id.equals(tiendaId))) return null;
        return contexto.tienda(id);
    }

    private static String volver(Long tiendaId, String tab) {
        return "redirect:/clientes?tiendaId=" + tiendaId + "&tab=" + tab;
    }

    // =============================== LISTA ===============================

    @GetMapping("/clientes")
    public String clientes(@RequestParam(required = false) Long tiendaId,
                           @RequestParam(defaultValue = "compradores") String tab,
                           Authentication auth, HttpSession session,
                           Model model, RedirectAttributes ra) {
        Tienda t = tiendaAdmin(auth, tiendaId, session);
        if (t == null) {
            ra.addFlashAttribute("mensajeError", "Clientes solo está disponible para administradores de tienda.");
            return "redirect:/home";
        }
        if (tiendaId == null) return volver(t.getId(), tab);

        model.addAttribute("tienda", t);
        model.addAttribute("tiendaId", t.getId());
        model.addAttribute("tab", tab);

        switch (tab) {
            case "frecuentes" -> {
                List<ClienteTienda> lista = frecuentes.frecuentes(t.getId());
                Map<Long, List<MovimientoFiado>> movs = new LinkedHashMap<>();
                for (ClienteTienda ct : lista) movs.put(ct.getId(), frecuentes.movimientos(ct.getId()));
                model.addAttribute("frecuentes", lista);
                model.addAttribute("movimientos", movs);
            }
            case "libreta" -> model.addAttribute("libreta", libretaRepo.findByTienda_IdOrderByNombreAsc(t.getId()));
            default -> {
                model.addAttribute("tab", "compradores");
                model.addAttribute("compradores", compraRepo.compradores(t.getId(), EstadoCompra.CONFIRMADA));
            }
        }
        return "clientes";
    }

    // ===================== FRECUENTES / FIADO (admin) =====================

    @PostMapping("/clientes/frecuentes/{id}/aprobar")
    public String aprobar(@PathVariable Long id, @RequestParam Long tiendaId,
                          @RequestParam(defaultValue = "false") boolean fiado,
                          @RequestParam(required = false) BigDecimal limite,
                          @RequestParam(required = false) String nota,
                          Authentication auth, RedirectAttributes ra) {
        Usuario admin = contexto.usuario(auth);
        if (!contexto.esAdmin(admin, tiendaId)) return sinPermiso(ra);
        try {
            frecuentes.aprobar(id, tiendaId, admin, fiado, limite, nota);
            ra.addFlashAttribute("mensajeExito", "Cliente frecuente aprobado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/notificaciones";
    }

    @PostMapping("/clientes/frecuentes/{id}/rechazar")
    public String rechazar(@PathVariable Long id, @RequestParam Long tiendaId,
                           @RequestParam(required = false) String nota,
                           Authentication auth, RedirectAttributes ra) {
        Usuario admin = contexto.usuario(auth);
        if (!contexto.esAdmin(admin, tiendaId)) return sinPermiso(ra);
        try {
            frecuentes.rechazar(id, tiendaId, admin, nota);
            ra.addFlashAttribute("mensajeExito", "Solicitud rechazada.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/notificaciones";
    }

    @PostMapping("/clientes/frecuentes/{id}/credito")
    public String credito(@PathVariable Long id, @RequestParam Long tiendaId,
                          @RequestParam(defaultValue = "false") boolean fiado,
                          @RequestParam(required = false) BigDecimal limite,
                          @RequestParam(defaultValue = "false") boolean suspender,
                          Authentication auth, RedirectAttributes ra) {
        if (!contexto.esAdmin(contexto.usuario(auth), tiendaId)) return sinPermiso(ra);
        try {
            frecuentes.actualizarCredito(id, tiendaId, fiado, limite, suspender);
            ra.addFlashAttribute("mensajeExito", "Crédito actualizado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return volver(tiendaId, "frecuentes");
    }

    @PostMapping("/clientes/frecuentes/{id}/abono")
    public String abono(@PathVariable Long id, @RequestParam Long tiendaId,
                        @RequestParam BigDecimal monto,
                        @RequestParam(required = false) String nota,
                        Authentication auth, RedirectAttributes ra) {
        Usuario admin = contexto.usuario(auth);
        if (!contexto.esAdmin(admin, tiendaId)) return sinPermiso(ra);
        try {
            frecuentes.registrarAbono(id, tiendaId, admin, monto, nota);
            ra.addFlashAttribute("mensajeExito", "Abono registrado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return volver(tiendaId, "frecuentes");
    }

    // ========================= LIBRETA (sin cuenta) =========================

    @GetMapping("/clientes/nuevo")
    public String nuevoCliente(@RequestParam Long tiendaId, Authentication auth, Model model, RedirectAttributes ra) {
        if (!contexto.esAdmin(contexto.usuario(auth), tiendaId)) return sinPermiso(ra);
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("esEdicion", false);
        return "nuevo_cliente";
    }

    @GetMapping("/clientes/editar/{id}")
    public String editarCliente(@PathVariable Long id, @RequestParam Long tiendaId,
                                Authentication auth, Model model, RedirectAttributes ra) {
        if (!contexto.esAdmin(contexto.usuario(auth), tiendaId)) return sinPermiso(ra);
        Cliente c = libretaRepo.findByIdAndTienda_Id(id, tiendaId).orElse(null);
        if (c == null) {
            ra.addFlashAttribute("mensajeError", "Cliente no encontrado en esta tienda.");
            return volver(tiendaId, "libreta");
        }
        model.addAttribute("cliente", c);
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("esEdicion", true);
        return "nuevo_cliente";
    }

    @PostMapping("/clientes/guardar")
    public String guardarCliente(@ModelAttribute Cliente form, @RequestParam Long tiendaId,
                                 Authentication auth, RedirectAttributes ra) {
        if (!contexto.esAdmin(contexto.usuario(auth), tiendaId)) return sinPermiso(ra);

        Cliente c;
        if (form.getId() != null) {
            // Solo se puede editar un cliente de TU tienda
            c = libretaRepo.findByIdAndTienda_Id(form.getId(), tiendaId).orElse(null);
            if (c == null) {
                ra.addFlashAttribute("mensajeError", "Cliente no encontrado en esta tienda.");
                return volver(tiendaId, "libreta");
            }
        } else {
            c = new Cliente();
            c.setTienda(contexto.tienda(tiendaId));
        }
        if (form.getNombre() == null || form.getNombre().isBlank()) {
            ra.addFlashAttribute("mensajeError", "El nombre es obligatorio.");
            return volver(tiendaId, "libreta");
        }
        c.setNombre(form.getNombre().trim());
        c.setEmail(form.getEmail());
        c.setTelefono(form.getTelefono());
        c.setDeudaMonto(form.getDeudaMonto());
        c.setDeudaNota(form.getDeudaNota());
        libretaRepo.save(c);
        ra.addFlashAttribute("mensajeExito", "Cliente guardado.");
        return volver(tiendaId, "libreta");
    }

    @PostMapping("/clientes/eliminar/{id}")
    public String eliminarCliente(@PathVariable Long id, @RequestParam Long tiendaId,
                                  Authentication auth, RedirectAttributes ra) {
        if (!contexto.esAdmin(contexto.usuario(auth), tiendaId)) return sinPermiso(ra);
        libretaRepo.findByIdAndTienda_Id(id, tiendaId).ifPresent(libretaRepo::delete);
        ra.addFlashAttribute("mensajeExito", "Cliente eliminado.");
        return volver(tiendaId, "libreta");
    }

    private static String sinPermiso(RedirectAttributes ra) {
        ra.addFlashAttribute("mensajeError", "No tienes permisos en esta tienda.");
        return "redirect:/home";
    }
}
