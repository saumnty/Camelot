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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Clientes.ClienteFrecuenteService;
import com.example.clientes_venta.Configuracion.ConfiguracionTiendaService;

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
    private final ConfiguracionTiendaService configService;
    private final ClienteFrecuenteService clienteFrecuenteService;

    public TiendaController(
            TiendaService tiendaService,
            TiendaRepository tiendaRepository,
            ProductoRepository productoRepository,
            TiendaProveedorRepository tiendaProveedorRepository,
            ProductoService productoService,
            UsuarioRepo usuarioRepo,
            TiendaSolicitudAdminService solicitudAdminService,
            ConfiguracionTiendaService configService,
            ClienteFrecuenteService clienteFrecuenteService
    ) {
        this.tiendaService = tiendaService;
        this.tiendaRepository = tiendaRepository;
        this.productoRepository = productoRepository;
        this.tiendaProveedorRepository = tiendaProveedorRepository;
        this.productoService = productoService;
        this.usuarioRepo = usuarioRepo;
        this.solicitudAdminService = solicitudAdminService;
        this.configService = configService;
        this.clienteFrecuenteService = clienteFrecuenteService;
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

        // ✅ Cliente frecuente / fiado
        model.addAttribute("permiteFrecuentes",
                t.getTipo() == TipoTienda.NORMAL && configService.de(t.getId()).isPermiteClientesFrecuentes());
        model.addAttribute("clienteFrecuente",
                authUser == null ? null : clienteFrecuenteService.de(t.getId(), authUser).orElse(null));

        // Ya NO se guarda la tienda visitada en sesión: eso hacía que la configuración
        // de "Abarrotes Mendoza" le apareciera a cualquier cliente que la visitaba.
        return "tienda_detalle";
    }


    // ✅ Cliente pide ser "cliente frecuente" de esta tienda → llega al buzón de los admins
    @PostMapping("/{id}/frecuente")
    public String solicitarFrecuente(@PathVariable Long id,
                                     @RequestParam(required = false) String nota,
                                     Authentication auth,
                                     RedirectAttributes ra) {
        Usuario u = auth == null ? null : usuarioRepo.findByEmail(auth.getName()).orElse(null);
        if (u == null) return "redirect:/login";
        Tienda t = tiendaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no existe"));

        String error = clienteFrecuenteService.solicitar(t, u, nota);
        if (error != null) {
            ra.addFlashAttribute("mensajeError", error);
        } else {
            ra.addFlashAttribute("mensajeExito", "Solicitud enviada. Te avisaremos en Notificaciones.");
        }
        return "redirect:/tiendas/" + id;
    }

    // ✅ FORM NUEVA TIENDA
    @GetMapping("/nuevo")
    public String nuevaTienda(Model model) {
        model.addAttribute("tienda", new Tienda());
        return "nueva_tienda";
    }

    // ✅ GUARDAR TIENDA + owner_id + fecha + tipo default
    @PostMapping("/guardar")
    public String guardarTienda(@ModelAttribute("tienda") Tienda tienda, Authentication auth, RedirectAttributes ra) {

        if (auth == null) return "redirect:/login";

        Usuario u = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        // Siempre es una tienda NUEVA: si el formulario trajera un id, se podría
        // sobrescribir la tienda de otra persona.
        tienda.setId(null);
        tienda.setActiva(true);
        tienda.setOwner(u);

        if (tienda.getFechaDeCreacion() == null) {
            tienda.setFechaDeCreacion(LocalDateTime.now());
        }

        if (tienda.getTipo() == null) {
            tienda.setTipo(TipoTienda.NORMAL);
        }

        Tienda nueva = tiendaService.guardarTienda(tienda);
        ra.addFlashAttribute("mensajeExito", "¡Listo! Tu negocio está creado. Agrega tus productos.");
        return "redirect:/productos?tiendaId=" + nueva.getId();
    }
}
