package com.example.clientes_venta.Carrito;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaRepository;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Controller
public class CarritoController {

    private final CarritoService carritoService;
    private final UsuarioRepo usuarioRepository;
    private final TiendaRepository tiendaRepository;

    public CarritoController(CarritoService carritoService, UsuarioRepo usuarioRepository,
                             TiendaRepository tiendaRepository) {
        this.carritoService = carritoService;
        this.usuarioRepository = usuarioRepository;
        this.tiendaRepository = tiendaRepository;
    }

    @PostMapping("/carrito/agregar/{productoId}")
    public String agregar(@PathVariable Long productoId, Authentication auth, HttpServletRequest request) {

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        carritoService.agregar(usuario.getId(), productoId);

        // ✅ regresar a la misma página donde estabas (catálogo proveedor / tienda / etc)
        String back = request.getHeader("Referer");

        return "redirect:" + rutaInterna(back, "/productos");
    }

    /**
     * El carrito es uno solo (de todas las tiendas). Con ?tiendaId=X se muestra solo lo de esa tienda
     * (es el "Mi carrito aquí" del submenú de la tienda).
     */
    @GetMapping("/carrito")
    public String verCarrito(@RequestParam(required = false) Long tiendaId,
                             Authentication auth, Model model, HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/productos")) {
            request.getSession().setAttribute("volverA", referer);
        }

        Usuario usuario = usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        Carrito carrito = carritoService.obtenerCarritoActivo(usuario.getId());
        List<CarritoItem> items = (carrito == null || carrito.getItems() == null) ? List.of() : carrito.getItems();
        Tienda filtro = null;
        if (tiendaId != null) {
            items = items.stream().filter(it -> tiendaId.equals(it.getProducto().getTienda().getId())).toList();
            filtro = tiendaRepository.findById(tiendaId).orElse(null);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CarritoItem it : items) {
            Producto p = it.getProducto();
            BigDecimal precio = (p.getPrecio() == null) ? BigDecimal.ZERO : p.getPrecio();
            total = total.add(precio.multiply(BigDecimal.valueOf(it.getCantidad())));
        }

        model.addAttribute("carrito", carrito);
        model.addAttribute("items", items);
        model.addAttribute("tiendaFiltro", filtro);
        model.addAttribute("total", total.setScale(2, RoundingMode.HALF_UP));
        return "carrito";
    }

    @PostMapping("/carrito/mas/{itemId}")
    public String mas(@PathVariable Long itemId, Authentication auth) {
        carritoService.incrementar(itemId, auth.getName());
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/menos/{itemId}")
    public String menos(@PathVariable Long itemId, Authentication auth) {
        carritoService.decrementar(itemId, auth.getName());
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/eliminar/{itemId}")
    public String eliminar(@PathVariable Long itemId, Authentication auth) {
        carritoService.eliminarItem(itemId, auth.getName());
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/limpiar")
    public String limpiar(Authentication auth) {
        carritoService.limpiar(auth.getName());
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/api/agregar/{productoId}")
    public ResponseEntity<?> agregarApi(@PathVariable Long productoId, Authentication auth) {

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        carritoService.agregar(usuario.getId(), productoId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/carrito/seguir-comprando")
    public String seguirComprando(HttpServletRequest request) {

        String back = (String) request.getSession().getAttribute("volverA");

        return "redirect:" + rutaInterna(back, "/productos");
    }

    /**
     * Del encabezado Referer (que manda el navegador) nos quedamos solo con la ruta interna.
     * Antes se redirigía a la URL completa: alguien podía mandar a la gente a otro sitio.
     */
    static String rutaInterna(String url, String porDefecto) {
        if (url == null || url.isBlank()) return porDefecto;
        try {
            URI u = URI.create(url);
            String path = u.getRawPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) return porDefecto;
            return u.getRawQuery() == null ? path : path + "?" + u.getRawQuery();
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }
}