package com.example.clientes_venta.Tienda;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tiendas")
public class TiendaProveedorRestController {

    private final TiendaProveedorService tiendaProveedorService;

    public TiendaProveedorRestController(TiendaProveedorService tiendaProveedorService) {
        this.tiendaProveedorService = tiendaProveedorService;
    }

    @GetMapping("/{tiendaId}/proveedores")
    public List<?> listar(@PathVariable Long tiendaId, Authentication auth) {
        return tiendaProveedorService.listarProveedores(tiendaId, auth).stream().map(tp -> new Object() {
            public final Long proveedorId = tp.getProveedor().getId();
            public final String proveedorNombre = tp.getProveedor().getNombre();
            public final String tipo = String.valueOf(tp.getProveedor().getTipo());
        }).toList();
    }
}
