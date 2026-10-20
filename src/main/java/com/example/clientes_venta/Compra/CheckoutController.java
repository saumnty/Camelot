package com.example.clientes_venta.Compra;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Pago.MetodoPago;
import com.example.clientes_venta.Pago.PagoRechazadoException;
import com.example.clientes_venta.Pago.PagoService;
import com.example.clientes_venta.Pago.ProveedorPago;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Controller
public class CheckoutController {

    private final UsuarioRepo usuarioRepo;
    private final CheckoutService checkoutService;
    private final PagoService pagoService;

    public CheckoutController(UsuarioRepo usuarioRepo, CheckoutService checkoutService, PagoService pagoService) {
        this.usuarioRepo = usuarioRepo;
        this.checkoutService = checkoutService;
        this.pagoService = pagoService;
    }

    private Usuario usuario(Authentication auth) {
        return usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));
    }

    @GetMapping("/checkout")
    public String checkoutPage(Authentication auth, Model model) {
        List<CheckoutService.GrupoTienda> grupos = checkoutService.resumen(usuario(auth));
        if (grupos.isEmpty()) return "redirect:/carrito";

        BigDecimal granTotal = grupos.stream()
                .map(g -> g.totales().total())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("grupos", grupos);
        model.addAttribute("granTotal", granTotal);
        model.addAttribute("pagoSimulado", pagoService.esSimulado());
        return "checkout";
    }

    /**
     * Recibe:
     *   metodo_{tiendaId} = TARJETA | EFECTIVO | FIADO   (uno por tienda)
     *   tarjetaNumero, tarjetaTitular, tarjetaVencimiento, tarjetaCvv  (si alguna va con tarjeta)
     */
    @PostMapping("/checkout")
    public String confirmarCheckout(@RequestParam Map<String, String> params,
                                    Authentication auth,
                                    RedirectAttributes redirect) {
        Usuario u = usuario(auth);

        Map<Long, MetodoPago> metodos = new HashMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("metodo_")) {
                try {
                    metodos.put(Long.valueOf(k.substring(7)), MetodoPago.valueOf(v));
                } catch (IllegalArgumentException ignored) {
                    // valor manipulado: se queda sin método y el servicio avisa
                }
            }
        });

        ProveedorPago.DatosTarjeta tarjeta = new ProveedorPago.DatosTarjeta(
                params.get("tarjetaNumero"), params.get("tarjetaTitular"),
                params.get("tarjetaVencimiento"), params.get("tarjetaCvv"), params.get("tarjetaToken"));

        List<Compra> compras;
        try {
            compras = checkoutService.checkout(u, metodos, tarjeta);
        } catch (PagoRechazadoException | IllegalArgumentException e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/checkout";
        }

        long confirmadas = compras.stream().filter(c -> c.getEstado() == EstadoCompra.CONFIRMADA).count();
        if (confirmadas == compras.size()) {
            redirect.addFlashAttribute("mensajeExito", "¡Compra confirmada!");
        } else if (confirmadas > 0) {
            redirect.addFlashAttribute("mensajeExito", "Algunos pedidos se confirmaron y otros esperan revisión de la tienda.");
        } else {
            redirect.addFlashAttribute("mensajeExito", "Pedido enviado. La tienda lo revisará y te avisaremos en Notificaciones.");
        }
        return "redirect:/compras";
    }
}
