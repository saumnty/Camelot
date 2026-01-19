package com.example.clientes_venta.Tienda;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tiendas")
public class TiendaProveedorController {

    private final TiendaProveedorService tiendaProveedorService;

    public TiendaProveedorController(TiendaProveedorService tiendaProveedorService) {
        this.tiendaProveedorService = tiendaProveedorService;
    }

    @PostMapping("/{tiendaId}/proveedores/{proveedorId}")
    public String agregar(@PathVariable Long tiendaId,
                          @PathVariable Long proveedorId,
                          Authentication auth) {
        tiendaProveedorService.agregarProveedor(tiendaId, proveedorId, auth);
        return "redirect:/tiendas/" + tiendaId + "/proveedores";
    }

    @PostMapping("/{tiendaId}/proveedores/{proveedorId}/quitar")
    public String quitar(@PathVariable Long tiendaId,
                         @PathVariable Long proveedorId,
                         Authentication auth) {
        tiendaProveedorService.quitarProveedor(tiendaId, proveedorId, auth);
        return "redirect:/tiendas/" + tiendaId + "/proveedores";
    }

    // Por ahora no hacemos vista, en el siguiente paso te doy una vista mínima
    // o lo cambiamos a @RestController para ver JSON.
}
