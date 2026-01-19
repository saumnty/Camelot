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
import java.util.Optional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.List;

import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;
import com.example.clientes_venta.Tienda.TiendaService;
import com.example.clientes_venta.CompraExterna.CompraExterna;
import com.example.clientes_venta.CompraExterna.CompraExternaRepository;

@Controller
@RequestMapping("/compras")
public class ComprasController {

    private final CompraService compraService;
    private final UsuarioRepo usuarioRepo;
    private final TiendaService tiendaService;
    private final CompraExternaRepository compraExternaRepo;

    public ComprasController(CompraService compraService, UsuarioRepo usuarioRepo,
                            TiendaService tiendaService, CompraExternaRepository compraExternaRepo) {
        this.compraService = compraService;
        this.usuarioRepo = usuarioRepo;
        this.tiendaService = tiendaService;
        this.compraExternaRepo = compraExternaRepo;
    }



    @GetMapping
    public String misCompras(Authentication auth, Model model) {
        Usuario usuario = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        List<Compra> compras = compraService.misCompras(usuario.getId());

        model.addAttribute("listaCompras", compras);
        model.addAttribute("paginaActual", "compras");
        model.addAttribute("nombre", usuario.getName());

        // ✅ Si es ADMIN de alguna tienda, habilitamos compra externa + reportes
        List<Long> adminTiendaIds = tiendaService.tiendasAdminIds(usuario);
        if (!adminTiendaIds.isEmpty()) {
            Long tiendaId = adminTiendaIds.get(0); // igual que tu idea de “primera tienda”
            model.addAttribute("tiendaId", tiendaId);

            // Mostrar compras externas del día (o puedes cambiar a últimas 7/30)
            LocalDate hoy = LocalDate.now();
            List<CompraExterna> externas = compraExternaRepo
                    .findByTienda_IdAndFechaOrderByCreatedAtDesc(tiendaId, hoy);

            model.addAttribute("listaExternas", externas);
            model.addAttribute("fechaExternas", hoy);
        }

        return "compras";
    }


    @GetMapping("/ticket/{id}")
    public String verTicket(@PathVariable Long id, Authentication auth, Model model) {

        Usuario usuario = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        // ✅ trae la compra (necesitas un método en service/repo)
        Compra compra = compraService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no existe"));

        // ✅ seguridad: que sea del usuario (si es cliente)
        if (!compra.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes ver este ticket");
        }

        model.addAttribute("compra", compra);
        model.addAttribute("paginaActual", "compras");
        model.addAttribute("nombre", usuario.getName());
        return "ticket"; // 👈 esto carga templates/ticket.html
    }
}
