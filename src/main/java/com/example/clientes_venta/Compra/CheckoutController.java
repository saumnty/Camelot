package com.example.clientes_venta.Compra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Carrito.Carrito;
import com.example.clientes_venta.Carrito.CarritoItem;
import com.example.clientes_venta.Carrito.CarritoService;
import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Controller
public class CheckoutController {

    private final UsuarioRepo usuarioRepo;
    private final CarritoService carritoService;
    private final CheckoutService checkoutService;

    public CheckoutController(UsuarioRepo usuarioRepo, CarritoService carritoService, CheckoutService checkoutService) {
        this.usuarioRepo = usuarioRepo;
        this.carritoService = carritoService;
        this.checkoutService = checkoutService;
    }

    @GetMapping("/checkout")
    public String checkoutPage(Authentication auth, Model model) {

        Usuario u = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        Carrito carrito = carritoService.obtenerCarritoActivo(u.getId());

        if (carrito == null || carrito.getItems() == null || carrito.getItems().isEmpty()) {
            return "redirect:/carrito";
        }

        model.addAttribute("carrito", carrito);

        BigDecimal total = BigDecimal.ZERO;

        for (CarritoItem it : carrito.getItems()) {
            Producto p = it.getProducto();

            // ✅ precio ya es BigDecimal (si viene null, lo tratamos como 0)
            BigDecimal precio = (p.getPrecio() == null) ? BigDecimal.ZERO : p.getPrecio();

            BigDecimal cantidad = BigDecimal.valueOf(it.getCantidad());
            BigDecimal subtotal = precio.multiply(cantidad);

            total = total.add(subtotal);
        }

        total = total.setScale(2, RoundingMode.HALF_UP);
        model.addAttribute("total", total);

        return "checkout";
    }

    @PostMapping("/checkout")
    public String confirmarCheckout(Authentication auth, RedirectAttributes redirect) {

        Usuario u = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        List<Compra> compras = checkoutService.checkout(u);
        boolean algunaConfirmada = compras.stream().anyMatch(c -> c.getEstado() == EstadoCompra.CONFIRMADA);
        boolean algunaPendiente = compras.stream().anyMatch(c -> c.getEstado() == EstadoCompra.PENDIENTE);

        if (algunaConfirmada && !algunaPendiente) {
            redirect.addFlashAttribute("success", "Compra confirmada con éxito");
        } else if (algunaConfirmada) {
            redirect.addFlashAttribute("success", "Algunas compras se confirmaron y otras quedaron pendientes por validación.");
        } else {
            redirect.addFlashAttribute("success", "Compra creada. Quedó pendiente por validación del ADMIN.");
        }
        return "redirect:/compras";
    }
}