package com.example.clientes_venta.Configuracion;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Tienda.EstadoMembresia;
import com.example.clientes_venta.Tienda.RolTienda;
import com.example.clientes_venta.Tienda.TiendaService;
import com.example.clientes_venta.Tienda.TiendaUsuarioRepository;
import com.example.clientes_venta.Usuario.UsuarioRepo;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionSistemaService configService;
    private final TiendaService tiendaService;

    // Resolver tienda por usuario logeado
    private final UsuarioRepo usuarioRepo;
    private final TiendaUsuarioRepository tiendaUsuarioRepo;

    @GetMapping("/config")
    public String mostrarFormularioConfig(
            Model model,
            HttpSession session,
            Authentication auth,
            @RequestParam(value = "tiendaId", required = false) Long tiendaId
    ) {
        Long tiendaIdResuelta = resolverTiendaId(tiendaId, session, auth);

        model.addAttribute("tiendaId", tiendaIdResuelta);

        ConfiguracionSistema config = configService.obtenerConfiguracion();

        // Opcional: mostrar el nombre REAL de la tienda activa en el input
        if (tiendaIdResuelta != null) {
            String nombreTienda = tiendaService.obtenerNombreTienda(tiendaIdResuelta);
            if (nombreTienda != null && !nombreTienda.isBlank()) {
                config.setNombreNegocio(nombreTienda);
            }
        }

        model.addAttribute("config", config);
        return "config";
    }

    @PostMapping("/config")
    public String guardarConfiguracion(
            @Valid @ModelAttribute("config") ConfiguracionSistema config,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            Authentication auth,
            @RequestParam(value = "tiendaId", required = false) Long tiendaId
    ) {
        Long tiendaIdResuelta = resolverTiendaId(tiendaId, session, auth);

        if (bindingResult.hasErrors()) {
            model.addAttribute("tiendaId", tiendaIdResuelta);
            model.addAttribute("config", config);
            return "config";
        }

        // 1) Guarda config global
        configService.guardar(config);

        // 2) Renombra tienda en BD (tienda.nombre)
        if (tiendaIdResuelta != null) {
            tiendaService.actualizarNombreTienda(tiendaIdResuelta, config.getNombreNegocio());
        } else {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Configuración guardada, pero no se pudo determinar la tienda para renombrarla.");
            return "redirect:/config";
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Configuración guardada correctamente.");
        return "redirect:/config?tiendaId=" + tiendaIdResuelta;
    }

    private Long resolverTiendaId(Long tiendaIdParam, HttpSession session, Authentication auth) {

        // A) Si viene por URL, esa manda (y se guarda en sesión)
        if (tiendaIdParam != null) {
            session.setAttribute("tiendaId", tiendaIdParam);
            return tiendaIdParam;
        }

        // B) Si ya hay en sesión, úsala
        Object tiendaIdObj = session.getAttribute("tiendaId");
        if (tiendaIdObj != null) {
            try {
                return (tiendaIdObj instanceof Long)
                        ? (Long) tiendaIdObj
                        : Long.valueOf(tiendaIdObj.toString());
            } catch (Exception ignored) {
                // sigue al fallback por usuario
            }
        }

        // C) Fallback: resolver por usuario logeado (ADMIN > CLIENTE)
        if (auth == null || auth.getName() == null) return null;

        var optU = usuarioRepo.findByEmail(auth.getName());
        if (optU.isEmpty()) return null;

        Integer userId = optU.get().getId();

        Long id = firstId(
                tiendaUsuarioRepo.findTiendaIdsByUsuarioRolEstado(
                        userId,
                        RolTienda.ADMIN,
                        EstadoMembresia.ACTIVO
                )
        );
        if (id != null) {
            session.setAttribute("tiendaId", id);
            return id;
        }

        id = firstId(
                tiendaUsuarioRepo.findTiendaIdsByUsuarioRolEstado(
                        userId,
                        RolTienda.CLIENTE,
                        EstadoMembresia.ACTIVO
                )
        );
        if (id != null) {
            session.setAttribute("tiendaId", id);
            return id;
        }

        return null;
    }

    private Long firstId(List<Long> ids) {
        return (ids != null && !ids.isEmpty()) ? ids.get(0) : null;
    }
}