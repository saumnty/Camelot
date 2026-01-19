package com.example.clientes_venta.Tienda;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Controller
public class HomeController {

    private final TiendaService tiendaService;
    private final UsuarioRepo usuarioRepository;

    public HomeController(TiendaService tiendaService, UsuarioRepo usuarioRepository) {
        this.tiendaService = tiendaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({"/", "/home"})
    public String home(@RequestParam(required = false) String q,
                       Model model,
                       Authentication auth) {

        String nombre = "Usuario";
        Usuario u = null;

        if (auth != null && auth.getName() != null) {
            u = usuarioRepository.findByEmail(auth.getName()).orElse(null);
            if (u != null && u.getName() != null && !u.getName().isBlank()) {
                nombre = u.getName();
            }
        }

        List<Tienda> tiendasVisibles = tiendaService.buscarVisibles(q, u);

        List<Tienda> tiendasNormales = tiendasVisibles.stream()
                .filter(t -> t.getTipo() == TipoTienda.NORMAL)
                .toList();

        List<Tienda> tiendasProveedoras = tiendasVisibles.stream()
                .filter(t -> t.getTipo() == TipoTienda.PROVEEDOR)
                .toList();

        model.addAttribute("nombre", nombre);
        model.addAttribute("q", q);

        // Para tu landing actual (si usa "tiendas"), deja SOLO las normales:
        model.addAttribute("tiendas", tiendasNormales);

        // Nuevas listas
        model.addAttribute("tiendasNormales", tiendasNormales);
        model.addAttribute("tiendasProveedoras", tiendasProveedoras);
        model.addAttribute("hayProveedoras", !tiendasProveedoras.isEmpty());

        return "landing";
    }
}
