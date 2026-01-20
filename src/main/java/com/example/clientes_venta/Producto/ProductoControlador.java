package com.example.clientes_venta.Producto;

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

import com.example.clientes_venta.Categoria.CategoriaService;
import com.example.clientes_venta.Subcategoria.SubcategoriaService;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaRepository;
import com.example.clientes_venta.Tienda.TiendaService;
import com.example.clientes_venta.Tienda.TipoTienda;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Controller
@RequestMapping("/productos")
public class ProductoControlador {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final SubcategoriaService subcategoriaService;
    private final UsuarioRepo usuarioRepo;
    private final TiendaService tiendaService;
    private final TiendaRepository tiendaRepo;

    public ProductoControlador(
            ProductoService productoService,
            CategoriaService categoriaService,
            SubcategoriaService subcategoriaService,
            UsuarioRepo usuarioRepo,
            TiendaService tiendaService,
            TiendaRepository tiendaRepo
    ) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.subcategoriaService = subcategoriaService;
        this.usuarioRepo = usuarioRepo;
        this.tiendaService = tiendaService;
        this.tiendaRepo = tiendaRepo;
    }

    private Usuario getUsuario(Authentication auth) {
        if (auth == null) return null;
        return usuarioRepo.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping
    public String listarProductos(
            @RequestParam(required = false) Long tiendaId,
            @RequestParam(required = false) String q,
            Model model,
            Authentication auth
    ) {
        Usuario u = getUsuario(auth);

        List<Long> adminTiendaIds = tiendaService.tiendasAdminIds(u);
        boolean adminDeAlguna = !adminTiendaIds.isEmpty();

        // --- Sin tiendaId ---
        if (tiendaId == null) {
            if (adminDeAlguna) {
                Long tiendaNormalId = tiendaRepo.findAllById(adminTiendaIds).stream()
                        .filter(t -> t.getTipo() == TipoTienda.NORMAL)
                        .map(Tienda::getId)
                        .findFirst()
                        .orElse(adminTiendaIds.get(0));
                return "redirect:/productos?tiendaId=" + tiendaNormalId;
            }

            model.addAttribute("productos", productoService.listarProductosMarketplace(q));
            model.addAttribute("catalogo", productoService.listarAgrupadoMarketplace(q));
            model.addAttribute("puedeGestionarProductos", false);
            model.addAttribute("adminTiendaIds", adminTiendaIds);
            model.addAttribute("q", q);
            return "productos";
        }

        // --- Con tiendaId ---
        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) return "redirect:/productos";

        if (tienda.getTipo() == TipoTienda.PROVEEDOR && !adminDeAlguna) {
            return "redirect:/home";
        }

        boolean puedeGestionar = tiendaService.esAdminDeTienda(u, tiendaId)
            && (tienda.getTipo() == TipoTienda.NORMAL || tienda.getTipo() == TipoTienda.PROVEEDOR);

        model.addAttribute("tienda", tienda);
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("puedeGestionarProductos", puedeGestionar);
        model.addAttribute("adminTiendaIds", adminTiendaIds);

        model.addAttribute("productos", productoService.listarProductosPorTienda(tiendaId));
        model.addAttribute("catalogo", productoService.listarAgrupadoPorTienda(tiendaId));
        model.addAttribute("q", q);

        return "productos";
    }

    @GetMapping("/nuevo")
    public String mostrarNuevo(
            @RequestParam Long tiendaId,
            Model model,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario u = getUsuario(auth);

        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/productos";
        }

        boolean puedeGestionar = tiendaService.esAdminDeTienda(u, tiendaId)
                && (tienda.getTipo() == TipoTienda.NORMAL || tienda.getTipo() == TipoTienda.PROVEEDOR);

        if (!puedeGestionar) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para agregar productos en esta tienda.");
            return "redirect:/productos?tiendaId=" + tiendaId;
        }

        Producto producto = new Producto();
        producto.setTienda(tienda);

        model.addAttribute("tienda", tienda);
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("producto", producto);
        model.addAttribute("esEdicion", false);
        model.addAttribute("categorias", categoriaService.listar());
        model.addAttribute("subcategorias", List.of());

        return "nuevo_producto";
    }


    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Producto producto,
            @RequestParam Long tiendaId,
            @RequestParam(required = false) String categoriaNombre,
            @RequestParam(required = false) String subcategoriaNombre,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario u = getUsuario(auth);

        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/productos";
        }

        boolean puedeGestionar = tiendaService.esAdminDeTienda(u, tiendaId)
            && (tienda.getTipo() == TipoTienda.NORMAL || tienda.getTipo() == TipoTienda.PROVEEDOR);

        if (!puedeGestionar) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para guardar productos en esta tienda.");
            return "redirect:/productos?tiendaId=" + tiendaId;
        }

        // ✅ blindar tienda
        producto.setTienda(tienda);

        productoService.guardarProducto(producto, categoriaNombre, subcategoriaNombre);

        ra.addFlashAttribute("mensajeExito", "Producto guardado correctamente.");
        return "redirect:/productos?tiendaId=" + tiendaId;
    }


    /**
     * ✅ EDITAR: solo validamos permisos y mandamos a tu vista actual "nuevo_producto"
     * (que trabaja con entity Producto).
     */
    @GetMapping("/editar/{id}")
    public String mostrarEditar(
            @PathVariable Long id,
            Model model,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario u = getUsuario(auth);

        Producto p = productoService.obtenerProductoPorId(id).orElse(null);
        if (p == null) {
            ra.addFlashAttribute("mensajeError", "Producto no encontrado.");
            return "redirect:/productos";
        }

        Long tiendaId = (p.getTienda() != null) ? p.getTienda().getId() : null;
        if (tiendaId == null) {
            ra.addFlashAttribute("mensajeError", "Producto sin tienda asociada.");
            return "redirect:/productos";
        }

        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/productos";
        }

        boolean puedeGestionar = tiendaService.esAdminDeTienda(u, tiendaId)
                && tienda.getTipo() == TipoTienda.NORMAL;

        if (!puedeGestionar) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para editar productos de esta tienda.");
            return "redirect:/productos?tiendaId=" + tiendaId;
        }

        model.addAttribute("tienda", tienda);
        model.addAttribute("producto", p);
        model.addAttribute("esEdicion", true);
        model.addAttribute("categorias", categoriaService.listar());
        model.addAttribute("tiendaId", tiendaId);

        if (p.getCategoria() != null && p.getCategoria().getId() != null) {
            model.addAttribute("subcategorias", subcategoriaService.listarPorCategoriaId(p.getCategoria().getId()));
        } else {
            model.addAttribute("subcategorias", List.of());
        }

        return "nuevo_producto";
    }

    /**
     * ✅ ELIMINAR: solo deja eliminar si es admin de la tienda normal.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id,
            @RequestParam(required = false) Long tiendaId,
            Authentication auth,
            RedirectAttributes ra
    ) {
        Usuario u = getUsuario(auth);

        Producto p = productoService.obtenerProductoPorId(id).orElse(null);
        if (p == null) {
            ra.addFlashAttribute("mensajeError", "Producto no encontrado.");
            return "redirect:/productos";
        }

        Long realTiendaId = (p.getTienda() != null) ? p.getTienda().getId() : tiendaId;
        if (realTiendaId == null) {
            ra.addFlashAttribute("mensajeError", "No se pudo determinar la tienda del producto.");
            return "redirect:/productos";
        }

        Tienda tienda = tiendaRepo.findById(realTiendaId).orElse(null);
        if (tienda == null) {
            ra.addFlashAttribute("mensajeError", "La tienda no existe.");
            return "redirect:/productos";
        }

        boolean puedeGestionar = tiendaService.esAdminDeTienda(u, realTiendaId)
                && tienda.getTipo() == TipoTienda.NORMAL;

        if (!puedeGestionar) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para eliminar productos de esta tienda.");
            return "redirect:/productos?tiendaId=" + realTiendaId;
        }

        try {
            productoService.eliminarProducto(id);
            ra.addFlashAttribute("mensajeExito", "Producto eliminado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "No se pudo eliminar: el producto está ligado a ventas/carrito.");
        }

        return "redirect:/productos?tiendaId=" + realTiendaId;
    }
}
