package com.example.clientes_venta.Tienda;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Producto.ProductoRepository;
import com.example.clientes_venta.Producto.ProductoService;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/tiendas")
public class TiendaController {

    private final TiendaService tiendaService;
    private final TiendaRepository tiendaRepository;
    private final ProductoRepository productoRepository;
    private final TiendaProveedorRepository tiendaProveedorRepository;
    private final ProductoService productoService;
    private final UsuarioRepo usuarioRepo;
    private final TiendaSolicitudAdminService solicitudAdminService;

    public TiendaController(
            TiendaService tiendaService,
            TiendaRepository tiendaRepository,
            ProductoRepository productoRepository,
            TiendaProveedorRepository tiendaProveedorRepository,
            ProductoService productoService,
            UsuarioRepo usuarioRepo,
            TiendaSolicitudAdminService solicitudAdminService
    ) {
        this.tiendaService = tiendaService;
        this.tiendaRepository = tiendaRepository;
        this.productoRepository = productoRepository;
        this.tiendaProveedorRepository = tiendaProveedorRepository;
        this.productoService = productoService;
        this.usuarioRepo = usuarioRepo;
        this.solicitudAdminService = solicitudAdminService;
    }

    // ✅ Detalle real de tienda/proveedora
    @GetMapping("/{id}")
    public String verTienda(@PathVariable Long id, Model model, Authentication auth, HttpSession session) {

        Tienda t = tiendaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no existe"));

        Usuario authUser = null;
        if (auth != null) {
            authUser = usuarioRepo.findByEmail(auth.getName()).orElse(null);
        }

        boolean adminDeAlguna = tiendaService.esAdminDeAlgunaTienda(authUser);

        // ✅ Permiso para ver catálogo: normal => todos; proveedor => solo admin
        boolean puedeVerCatalogo = (t.getTipo() == TipoTienda.NORMAL) || adminDeAlguna;
        model.addAttribute("puedeVerCatalogo", puedeVerCatalogo);

        model.addAttribute("tienda", t);
        model.addAttribute("catalogoUrl", "/productos?tiendaId=" + t.getId());

        List<Producto> top = List.of();
        if (t.getTipo() == TipoTienda.NORMAL || adminDeAlguna) {
            top = productoService.topProductosParaDetalle(t.getId());
        }
        model.addAttribute("topProductos", top);

        if (t.getTipo() == TipoTienda.NORMAL) {
            List<TiendaProveedor> provs = tiendaProveedorRepository
                    .findByTienda_IdAndActivoTrueOrderByCreadoEnDesc(t.getId());
            model.addAttribute("proveedores", provs);
        }

        // ✅ ====== NUEVO: variables para "Soy dueño" ======
        boolean yaEsAdminDeEstaTienda = false;
        boolean solicitudPendiente = false;

        if (authUser != null) {
            yaEsAdminDeEstaTienda = solicitudAdminService.yaEsAdmin(t.getId(), authUser);
            solicitudPendiente = solicitudAdminService.tieneSolicitudPendiente(t.getId(), authUser);
        }

        model.addAttribute("authUser", authUser);
        model.addAttribute("yaEsAdminDeEstaTienda", yaEsAdminDeEstaTienda);
        model.addAttribute("solicitudPendiente", solicitudPendiente);
        // ✅ ==============================================

        // guarda la tienda actual en sesión (clave que /config lee)
        session.setAttribute("tiendaId", id);

        return "tienda_detalle";
    }


    // ✅ FORM NUEVA TIENDA
    @GetMapping("/nuevo")
    public String nuevaTienda(Model model) {
        model.addAttribute("tienda", new Tienda());
        return "nueva_tienda";
    }

    // ✅ GUARDAR TIENDA + owner_id + fecha + tipo default
    @PostMapping("/guardar")
    public String guardarTienda(@ModelAttribute("tienda") Tienda tienda, Authentication auth) {

        if (auth == null) return "redirect:/login";

        Usuario u = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        tienda.setOwner(u);

        if (tienda.getFechaDeCreacion() == null) {
            tienda.setFechaDeCreacion(LocalDateTime.now());
        }

        if (tienda.getTipo() == null) {
            tienda.setTipo(TipoTienda.NORMAL);
        }

        tiendaService.guardarTienda(tienda);
        return "redirect:/home";
    }
}
