package com.example.clientes_venta.Configuracion;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaContexto;
import com.example.clientes_venta.Tienda.TiendaRepository;
import com.example.clientes_venta.Tienda.TiendaService;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Configuración separada en dos:
 *   /config/cuenta  → cualquier usuario: su nombre, teléfono y contraseña.
 *   /config/tienda  → SOLO admins de esa tienda: datos del negocio, IVA, pagos, etc.
 */
@Controller
public class ConfiguracionController {

    private final TiendaContexto contexto;
    private final TiendaRepository tiendaRepo;
    private final TiendaService tiendaService;
    private final ConfiguracionTiendaService configService;
    private final UsuarioRepo usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public ConfiguracionController(TiendaContexto contexto,
                                   TiendaRepository tiendaRepo,
                                   TiendaService tiendaService,
                                   ConfiguracionTiendaService configService,
                                   UsuarioRepo usuarioRepo,
                                   PasswordEncoder passwordEncoder) {
        this.contexto = contexto;
        this.tiendaRepo = tiendaRepo;
        this.tiendaService = tiendaService;
        this.configService = configService;
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // Enlaces viejos a /config siguen funcionando
    @GetMapping("/config")
    public String config() {
        return "redirect:/config/cuenta";
    }

    // =========================== MI CUENTA ===========================

    @GetMapping("/config/cuenta")
    public String cuenta(Authentication auth, Model model) {
        Usuario u = contexto.usuario(auth);
        if (u == null) return "redirect:/login";
        model.addAttribute("usuario", u);
        return "config_cuenta";
    }

    @PostMapping("/config/cuenta")
    @Transactional
    public String guardarCuenta(@RequestParam String name,
                                @RequestParam(required = false) String telefono,
                                Authentication auth,
                                RedirectAttributes ra) {
        Usuario u = contexto.usuario(auth);
        if (u == null) return "redirect:/login";

        String nombre = name == null ? "" : name.trim();
        if (nombre.isBlank() || nombre.length() > 120) {
            ra.addFlashAttribute("mensajeError", "El nombre es obligatorio (máximo 120 caracteres).");
            return "redirect:/config/cuenta";
        }
        String tel = ConfigTiendaForm.limpio(telefono);
        if (tel != null && tel.length() > 30) {
            ra.addFlashAttribute("mensajeError", "El teléfono es demasiado largo.");
            return "redirect:/config/cuenta";
        }
        u.setName(nombre);
        u.setTelefono(tel);
        usuarioRepo.save(u);
        ra.addFlashAttribute("mensajeExito", "Datos de tu cuenta guardados.");
        return "redirect:/config/cuenta";
    }

    @PostMapping("/config/cuenta/password")
    @Transactional
    public String cambiarPassword(@RequestParam String actual,
                                  @RequestParam String nueva,
                                  @RequestParam String confirmar,
                                  Authentication auth,
                                  RedirectAttributes ra) {
        Usuario u = contexto.usuario(auth);
        if (u == null) return "redirect:/login";

        if (!passwordEncoder.matches(actual, u.getPassword())) {
            ra.addFlashAttribute("mensajeError", "La contraseña actual no es correcta.");
        } else if (nueva == null || nueva.length() < 6) {
            ra.addFlashAttribute("mensajeError", "La nueva contraseña debe tener al menos 6 caracteres.");
        } else if (!nueva.equals(confirmar)) {
            ra.addFlashAttribute("mensajeError", "La confirmación no coincide.");
        } else {
            u.setPassword(passwordEncoder.encode(nueva));
            usuarioRepo.save(u);
            ra.addFlashAttribute("mensajeExito", "Contraseña actualizada.");
        }
        return "redirect:/config/cuenta";
    }

    // ======================= CONFIGURACIÓN DE TIENDA =======================

    @GetMapping("/config/tienda")
    public String configTienda(@RequestParam(required = false) Long tiendaId,
                               Authentication auth,
                               HttpSession session,
                               Model model,
                               RedirectAttributes ra) {
        Usuario u = contexto.usuario(auth);
        Long id = contexto.resolverTiendaAdmin(u, tiendaId, session);
        if (id == null || (tiendaId != null && !id.equals(tiendaId))) {
            ra.addFlashAttribute("mensajeError", "Solo los administradores de la tienda pueden ver su configuración.");
            return "redirect:/config/cuenta";
        }
        Tienda t = tiendaRepo.findById(id).orElseThrow();
        ConfiguracionTienda c = configService.de(id);

        model.addAttribute("tienda", t);
        model.addAttribute("tiendaId", id);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", ConfigTiendaForm.desde(t, c));
        }
        return "config_tienda";
    }

    @PostMapping("/config/tienda")
    @Transactional
    public String guardarConfigTienda(@RequestParam Long tiendaId,
                                      @Valid @ModelAttribute("form") ConfigTiendaForm form,
                                      BindingResult br,
                                      Authentication auth,
                                      Model model,
                                      RedirectAttributes ra) {
        Usuario u = contexto.usuario(auth);
        // Antes cualquiera podía renombrar cualquier tienda: ahora se valida siempre
        if (!contexto.esAdmin(u, tiendaId)) {
            ra.addFlashAttribute("mensajeError", "No tienes permisos para configurar esta tienda.");
            return "redirect:/config/cuenta";
        }
        Tienda t = tiendaRepo.findById(tiendaId).orElseThrow();

        if (!br.hasErrors() && !(form.isAceptaTarjeta() || form.isAceptaEfectivo() || form.isAceptaFiado())) {
            br.reject("pagos", "Deja activa al menos una forma de pago.");
        }
        if (br.hasErrors()) {
            model.addAttribute("tienda", t);
            model.addAttribute("tiendaId", tiendaId);
            return "config_tienda";
        }

        // 1) Datos visibles de la tienda
        String nuevoNombre = form.getNombre().trim();
        if (!nuevoNombre.equals(t.getNombre())) {
            t.setNombre(nuevoNombre);
            t.setSlug(TiendaService.slug(nuevoNombre));
        }
        t.setDescripcion(ConfigTiendaForm.limpio(form.getDescripcion()));
        t.setCategoria(ConfigTiendaForm.limpio(form.getCategoria()));
        t.setCiudad(ConfigTiendaForm.limpio(form.getCiudad()));
        t.setGoogleMapsUrl(ConfigTiendaForm.limpio(form.getGoogleMapsUrl()));
        t.setActiva(form.isActiva());
        tiendaRepo.save(t);

        // 2) Configuración propia de la tienda
        ConfiguracionTienda c = configService.obtenerOCrear(tiendaId);
        form.aplicarA(c);
        configService.guardar(c);

        ra.addFlashAttribute("mensajeExito", "Configuración de la tienda guardada.");
        return "redirect:/config/tienda?tiendaId=" + tiendaId;
    }
}
