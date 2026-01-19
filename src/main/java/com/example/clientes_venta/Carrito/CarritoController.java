package com.example.clientes_venta.Carrito;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpServletRequest;

import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Controller
public class CarritoController {

    private final CarritoService carritoService;
    private final UsuarioRepo usuarioRepository;

    public CarritoController(CarritoService carritoService, UsuarioRepo usuarioRepository) {
        this.carritoService = carritoService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/carrito/agregar/{productoId}")
    public String agregar(@PathVariable Long productoId, Authentication auth, HttpServletRequest request) {

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        carritoService.agregar(usuario.getId(), productoId);

        // ✅ regresar a la misma página donde estabas (catálogo proveedor / tienda / etc)
        String back = request.getHeader("Referer");

        // si no hay referer, fallback
        return "redirect:" + (back != null ? back : "/productos");
    }

    @GetMapping("/carrito")
    public String verCarrito(Authentication auth, Model model, CsrfToken token, HttpServletRequest request) {

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/productos")) {
            request.getSession().setAttribute("volverA", referer);
        }

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        Carrito carrito = carritoService.obtenerCarritoActivo(usuario.getId());
        model.addAttribute("carrito", carrito);

        BigDecimal total = BigDecimal.ZERO;

        if (carrito != null && carrito.getItems() != null) {
            for (CarritoItem it : carrito.getItems()) {
                Producto p = it.getProducto();

                BigDecimal precio = (p.getPrecio() == null) ? BigDecimal.ZERO : p.getPrecio();
                BigDecimal cantidad = BigDecimal.valueOf(it.getCantidad());

                total = total.add(precio.multiply(cantidad));
            }
        }

        total = total.setScale(2, RoundingMode.HALF_UP);
        model.addAttribute("total", total);

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

        return "redirect:" + (back != null ? back : "/productos");
    }

}