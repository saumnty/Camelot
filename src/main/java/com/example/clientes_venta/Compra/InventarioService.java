package com.example.clientes_venta.Compra;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Configuracion.ConfiguracionTienda;
import com.example.clientes_venta.Configuracion.ConfiguracionTiendaService;
import com.example.clientes_venta.Notificacion.NotificacionService;
import com.example.clientes_venta.Notificacion.TipoNotificacion;
import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Producto.ProductoRepository;

/**
 * Movimientos de stock de una compra. Regla del proyecto: stock NULL = "no lo llevo contado",
 * no se descuenta ni bloquea la venta (el admin decide al confirmar).
 */
@Service
public class InventarioService {

    private final ProductoRepository productoRepo;
    private final ConfiguracionTiendaService configService;
    private final NotificacionService notificaciones;

    public InventarioService(ProductoRepository productoRepo,
                             ConfiguracionTiendaService configService,
                             NotificacionService notificaciones) {
        this.productoRepo = productoRepo;
        this.configService = configService;
        this.notificaciones = notificaciones;
    }

    /** ¿Todos los productos tienen stock contado y suficiente? (bloquea las filas mientras dura la transacción) */
    @Transactional
    public boolean alcanzaParaAutoConfirmar(Compra compra) {
        for (CompraItem it : compra.getItems()) {
            Producto p = bloquear(it.getProducto().getId());
            if (p.getStock() == null || p.getStock() < it.getCantidad()) return false;
        }
        return true;
    }

    /**
     * Descuenta el stock al confirmar. Lanza IllegalArgumentException si un producto
     * con stock contado no alcanza (el admin debe editar o cancelar el pedido).
     */
    @Transactional
    public void descontar(Compra compra) {
        List<Producto> bloqueados = new ArrayList<>();
        for (CompraItem it : compra.getItems()) {
            Producto p = bloquear(it.getProducto().getId());
            if (p.getStock() != null && p.getStock() < it.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente de \"" + p.getName() + "\": hay "
                        + p.getStock() + " y el pedido pide " + it.getCantidad() + ".");
            }
            bloqueados.add(p);
        }
        for (int i = 0; i < bloqueados.size(); i++) {
            Producto p = bloqueados.get(i);
            if (p.getStock() != null) {
                p.setStock(p.getStock() - compra.getItems().get(i).getCantidad());
                productoRepo.save(p);
            }
        }
        avisarStockBajo(compra, bloqueados);
    }

    /** Regresa el stock (reembolso de una compra ya confirmada). */
    @Transactional
    public void devolver(Compra compra) {
        for (CompraItem it : compra.getItems()) {
            Producto p = bloquear(it.getProducto().getId());
            if (p.getStock() != null) {
                p.setStock(p.getStock() + it.getCantidad());
                productoRepo.save(p);
            }
        }
    }

    private Producto bloquear(Long productoId) {
        return productoRepo.findByIdForUpdate(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no existe: " + productoId));
    }

    private void avisarStockBajo(Compra compra, List<Producto> productos) {
        ConfiguracionTienda cfg = configService.de(compra.getTienda().getId());
        Integer umbral = cfg.getStockBajoUmbral();
        if (umbral == null) return;

        List<String> bajos = productos.stream()
                .filter(p -> p.getStock() != null && p.getStock() <= umbral)
                .map(p -> p.getName() + " (" + p.getStock() + ")")
                .distinct()
                .toList();
        if (bajos.isEmpty()) return;

        notificaciones.notificarAdmins(compra.getTienda(), TipoNotificacion.STOCK_BAJO,
                "Stock bajo en " + compra.getTienda().getNombre(),
                String.join(", ", bajos),
                "/productos?tiendaId=" + compra.getTienda().getId(), compra.getId());
    }
}
