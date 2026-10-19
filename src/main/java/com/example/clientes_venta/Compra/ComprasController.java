package com.example.clientes_venta.Compra;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.example.clientes_venta.Configuracion.ConfiguracionTiendaService;
import com.example.clientes_venta.Pago.PagoService;
import com.example.clientes_venta.Tienda.TiendaService;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

/** "Mis compras" del cliente. Lo de administración (gastos, reportes) ahora vive en la sección "Mi tienda". */
@Controller
@RequestMapping("/compras")
public class ComprasController {

    private final CompraService compraService;
    private final UsuarioRepo usuarioRepo;
    private final TiendaService tiendaService;
    private final ConfiguracionTiendaService configService;
    private final PagoService pagoService;

    public ComprasController(CompraService compraService, UsuarioRepo usuarioRepo, TiendaService tiendaService,
                             ConfiguracionTiendaService configService, PagoService pagoService) {
        this.compraService = compraService;
        this.usuarioRepo = usuarioRepo;
        this.tiendaService = tiendaService;
        this.configService = configService;
        this.pagoService = pagoService;
    }

    private Usuario usuario(Authentication auth) {
        return usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));
    }

    @GetMapping
    public String misCompras(Authentication auth, Model model) {
        Usuario usuario = usuario(auth);
        List<Compra> compras = compraService.misCompras(usuario.getId());
        model.addAttribute("listaCompras", compras);
        model.addAttribute("paginaActual", "compras");
        return "compras";
    }

    @GetMapping("/ticket/{id}")
    public String verTicket(@PathVariable Long id, Authentication auth, Model model) {
        Usuario usuario = usuario(auth);
        Compra compra = compraService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no existe"));

        // Lo puede ver quien compró o un admin de la tienda que vendió
        boolean esComprador = compra.getUsuario().getId().equals(usuario.getId());
        boolean esAdminTienda = tiendaService.esAdminDeTienda(usuario, compra.getTienda().getId());
        if (!esComprador && !esAdminTienda) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes ver este ticket");
        }

        model.addAttribute("compra", compra);
        model.addAttribute("config", configService.de(compra.getTienda().getId()));
        model.addAttribute("pagoTarjeta", pagoService.cobroTarjeta(compra.getId()).orElse(null));
        model.addAttribute("paginaActual", "compras");
        return "ticket";
    }
}
