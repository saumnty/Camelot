package com.example.clientes_venta.Producto.importar;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaContexto;
import com.example.clientes_venta.Usuario.Usuario;

import jakarta.servlet.http.HttpSession;

/**
 * Importar productos desde Excel/CSV y autollenado por código de barras. Solo admins de la tienda.
 *
 *   GET  /productos/importar?tiendaId=     formulario
 *   GET  /productos/importar/plantilla.csv plantilla de ejemplo
 *   POST /productos/importar               sube archivo → vista previa (no guarda nada)
 *   POST /productos/importar/confirmar     guarda lo de la vista previa
 *   GET  /api/productos/codigo/{codigo}    JSON para autollenar el formulario de producto
 */
@Controller
public class ImportacionController {

    private static final String SESION = "importacionProductos";

    /** Lo que queda en sesión entre la vista previa y "Confirmar". */
    record Pendiente(Long tiendaId, String archivo, ArrayList<FilaImportada> filas) implements Serializable {}

    private final TiendaContexto contexto;
    private final ImportacionProductosService importacion;
    private final CodigoBarrasService codigos;

    public ImportacionController(TiendaContexto contexto,
                                 ImportacionProductosService importacion,
                                 CodigoBarrasService codigos) {
        this.contexto = contexto;
        this.importacion = importacion;
        this.codigos = codigos;
    }

    private Tienda tiendaAdmin(Authentication auth, Long tiendaId) {
        Usuario u = contexto.usuario(auth);
        return contexto.esAdmin(u, tiendaId) ? contexto.tienda(tiendaId) : null;
    }

    @GetMapping("/productos/importar")
    public String formulario(@RequestParam Long tiendaId, Authentication auth, Model model, RedirectAttributes ra) {
        Tienda t = tiendaAdmin(auth, tiendaId);
        if (t == null) return sinPermiso(ra);
        model.addAttribute("tienda", t);
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("maxBusquedas", ImportacionProductosService.MAX_BUSQUEDAS);
        return "productos_importar";
    }

    @GetMapping("/productos/importar/plantilla.csv")
    public ResponseEntity<byte[]> plantilla() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla_productos_camelot.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(ImportacionProductosService.plantillaCsv().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/productos/importar")
    public String vistaPrevia(@RequestParam Long tiendaId,
                              @RequestParam("archivo") MultipartFile archivo,
                              @RequestParam(defaultValue = "false") boolean completar,
                              @RequestParam(defaultValue = "false") boolean actualizar,
                              Authentication auth, HttpSession session,
                              Model model, RedirectAttributes ra) {
        Tienda t = tiendaAdmin(auth, tiendaId);
        if (t == null) return sinPermiso(ra);
        if (archivo == null || archivo.isEmpty()) {
            ra.addFlashAttribute("mensajeError", "Elige un archivo para importar.");
            return "redirect:/productos/importar?tiendaId=" + tiendaId;
        }

        ImportacionProductosService.VistaPrevia vista;
        try {
            List<List<String>> tabla = importacion.leerArchivo(archivo);
            vista = importacion.previsualizar(t, tabla, completar, actualizar);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/productos/importar?tiendaId=" + tiendaId;
        } catch (IOException | RuntimeException e) {
            ra.addFlashAttribute("mensajeError", "No pude leer el archivo. ¿Es un Excel (.xlsx) o CSV válido? "
                    + "Si viene de otro programa, ábrelo en Excel y guárdalo como .xlsx o .csv.");
            return "redirect:/productos/importar?tiendaId=" + tiendaId;
        }

        session.setAttribute(SESION, new Pendiente(tiendaId, archivo.getOriginalFilename(), new ArrayList<>(vista.filas())));
        model.addAttribute("tienda", t);
        model.addAttribute("tiendaId", tiendaId);
        model.addAttribute("vista", vista);
        model.addAttribute("archivo", archivo.getOriginalFilename());
        model.addAttribute("maxBusquedas", ImportacionProductosService.MAX_BUSQUEDAS);
        return "productos_importar";
    }

    @PostMapping("/productos/importar/confirmar")
    public String confirmar(@RequestParam Long tiendaId, Authentication auth, HttpSession session, RedirectAttributes ra) {
        Tienda t = tiendaAdmin(auth, tiendaId);
        if (t == null) return sinPermiso(ra);

        Object o = session.getAttribute(SESION);
        if (!(o instanceof Pendiente p) || !tiendaId.equals(p.tiendaId())) {
            ra.addFlashAttribute("mensajeError", "La vista previa expiró. Vuelve a subir el archivo.");
            return "redirect:/productos/importar?tiendaId=" + tiendaId;
        }
        try {
            ImportacionProductosService.Resultado r = importacion.aplicar(t, p.filas());
            session.removeAttribute(SESION);
            ra.addFlashAttribute("mensajeExito", "Importación lista: " + r.creados() + " productos nuevos y "
                    + r.actualizados() + " actualizados.");
            return "redirect:/productos?tiendaId=" + tiendaId;
        } catch (RuntimeException e) {
            ra.addFlashAttribute("mensajeError", "No se guardó nada: " + e.getMessage());
            return "redirect:/productos/importar?tiendaId=" + tiendaId;
        }
    }

    @PostMapping("/productos/importar/cancelar")
    public String cancelar(@RequestParam Long tiendaId, HttpSession session) {
        session.removeAttribute(SESION);
        return "redirect:/productos/importar?tiendaId=" + tiendaId;
    }

    /** Autollenado del formulario de producto. Solo para quien administra alguna tienda. */
    @GetMapping("/api/productos/codigo/{codigo}")
    @ResponseBody
    public ResponseEntity<?> buscarCodigo(@PathVariable String codigo, Authentication auth) {
        Usuario u = contexto.usuario(auth);
        if (contexto.tiendasQueAdministra(u).isEmpty()) return ResponseEntity.status(403).build();
        String limpio = Medidas.codigoBarras(codigo);
        if (!Medidas.codigoValido(limpio)) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El código debe tener 8, 12, 13 o 14 dígitos."));
        }
        return codigos.buscar(limpio)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("mensaje", "No encontré ese código. Llena los datos a mano.")));
    }

    private static String sinPermiso(RedirectAttributes ra) {
        ra.addFlashAttribute("mensajeError", "Solo los administradores de la tienda pueden importar productos.");
        return "redirect:/home";
    }
}
