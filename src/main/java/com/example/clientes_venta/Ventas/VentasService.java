package com.example.clientes_venta.Ventas;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Clientes.ClienteFrecuenteService;
import com.example.clientes_venta.Compra.Compra;
import com.example.clientes_venta.Compra.CompraItem;
import com.example.clientes_venta.Compra.CompraRepository;
import com.example.clientes_venta.Compra.EstadoCompra;
import com.example.clientes_venta.Compra.InventarioService;
import com.example.clientes_venta.Configuracion.ConfiguracionTienda;
import com.example.clientes_venta.Configuracion.ConfiguracionTiendaService;
import com.example.clientes_venta.Notificacion.NotificacionService;
import com.example.clientes_venta.Notificacion.TipoNotificacion;
import com.example.clientes_venta.Pago.EstadoPago;
import com.example.clientes_venta.Pago.MetodoPago;
import com.example.clientes_venta.Pago.PagoService;
import com.example.clientes_venta.Pago.Totales;
import com.example.clientes_venta.Usuario.Usuario;

/**
 * Lo que el ADMIN hace con los pedidos de su tienda.
 *
 *   PENDIENTE ──confirmar──▶ CONFIRMADA ──reembolsar──▶ REEMBOLSADA
 *       │                                    (si la tienda lo permite)
 *       └──cancelar──▶ CANCELADA (sin cobro) / REEMBOLSADA (si ya se había pagado con tarjeta)
 *
 * Todas las acciones reciben el tiendaId y verifican que el pedido sea de ESA tienda
 * (antes un admin podía aprobar pedidos de otra tienda cambiando el id en la URL).
 */
@Service
public class VentasService {

    private final CompraRepository compraRepo;
    private final InventarioService inventario;
    private final PagoService pagoService;
    private final ClienteFrecuenteService frecuentes;
    private final ConfiguracionTiendaService configService;
    private final NotificacionService notificaciones;

    public VentasService(CompraRepository compraRepo,
                         InventarioService inventario,
                         PagoService pagoService,
                         ClienteFrecuenteService frecuentes,
                         ConfiguracionTiendaService configService,
                         NotificacionService notificaciones) {
        this.compraRepo = compraRepo;
        this.inventario = inventario;
        this.pagoService = pagoService;
        this.frecuentes = frecuentes;
        this.configService = configService;
        this.notificaciones = notificaciones;
    }

    public List<Compra> ventasPendientesPorTienda(Long tiendaId) {
        return compraRepo.findByTienda_IdAndEstadoOrderByFechaDesc(tiendaId, EstadoCompra.PENDIENTE);
    }

    public List<Compra> ventasPorTienda(Long tiendaId) {
        return compraRepo.findByTienda_IdOrderByFechaDesc(tiendaId);
    }

    // =============================== CONFIRMAR ===============================

    @Transactional
    public void aprobarVenta(Long compraId, Long tiendaId, Usuario admin) {
        Compra c = deLaTienda(compraId, tiendaId);
        if (c.getEstado() != EstadoCompra.PENDIENTE) {
            throw new IllegalArgumentException("El pedido #" + compraId + " ya no está pendiente.");
        }

        // 1) Stock (lanza si no alcanza → no se confirma nada)
        inventario.descontar(c);

        // 2) Fiado: se carga a la cuenta del cliente en este momento
        if (c.getMetodoPago() == MetodoPago.FIADO) {
            frecuentes.cargar(tiendaId, c.getUsuario(), c.getTotal(), c.getId(), admin);
            c.setEstadoPago(EstadoPago.A_CUENTA);
        }

        c.setEstado(EstadoCompra.CONFIRMADA);
        compraRepo.save(c);

        notificaciones.resolverReferencia(TipoNotificacion.COMPRA_NUEVA, c.getId());
        notificaciones.notificar(c.getUsuario(), c.getTienda(), TipoNotificacion.COMPRA_CONFIRMADA,
                "Tu pedido #" + c.getId() + " en " + c.getTienda().getNombre() + " está confirmado",
                c.getMetodoPago() == MetodoPago.EFECTIVO ? "Paga $" + c.getTotal() + " al recoger." : "Total $" + c.getTotal(),
                "/compras/ticket/" + c.getId(), c.getId());
    }

    // =============================== CANCELAR ===============================

    /** Cancela un pedido PENDIENTE. Si ya estaba pagado con tarjeta, se devuelve el dinero. */
    @Transactional
    public void cancelarVenta(Long compraId, Long tiendaId, String motivo) {
        Compra c = deLaTienda(compraId, tiendaId);
        if (c.getEstado() != EstadoCompra.PENDIENTE) {
            throw new IllegalArgumentException("Solo se pueden cancelar pedidos pendientes. Para uno confirmado usa \"Reembolsar\".");
        }
        c.setMotivo(limpio(motivo));

        if (c.getEstadoPago() == EstadoPago.PAGADO && c.getMetodoPago() == MetodoPago.TARJETA) {
            // No se puede quedar con el dinero de un pedido que no se entregó: siempre se devuelve.
            pagoService.reembolsarTarjeta(c);
            c.setEstado(EstadoCompra.REEMBOLSADA);
            c.setEstadoPago(EstadoPago.REEMBOLSADO);
        } else {
            c.setEstado(EstadoCompra.CANCELADA);
            if (c.getEstadoPago() != null) c.setEstadoPago(EstadoPago.CANCELADO);
        }
        compraRepo.save(c);
        avisarCliente(c, c.getEstado() == EstadoCompra.REEMBOLSADA ? "se canceló y se te reembolsó" : "fue cancelado");
    }

    // ============================== REEMBOLSAR ==============================

    /** Devolución de un pedido ya CONFIRMADO. Solo si la tienda permite reembolsos. */
    @Transactional
    public void reembolsarVenta(Long compraId, Long tiendaId, String motivo, Usuario admin) {
        Compra c = deLaTienda(compraId, tiendaId);
        if (c.getEstado() != EstadoCompra.CONFIRMADA) {
            throw new IllegalArgumentException("Solo se reembolsan pedidos confirmados.");
        }
        ConfiguracionTienda cfg = configService.de(tiendaId);
        if (!cfg.isPermiteReembolsos()) {
            throw new IllegalArgumentException("Tu tienda tiene los reembolsos desactivados (Configuración de la tienda).");
        }

        EstadoPago ep = c.getEstadoPago();
        if (ep == EstadoPago.PAGADO && c.getMetodoPago() == MetodoPago.TARJETA) {
            pagoService.reembolsarTarjeta(c);
        } else if (ep == EstadoPago.A_CUENTA) {
            frecuentes.revertirCargo(tiendaId, c.getUsuario(), c.getTotal(), c.getId(), admin);
        }
        // EFECTIVO ya cobrado: el dinero se devuelve en mostrador; aquí solo se registra.

        inventario.devolver(c);

        boolean habiaCobro = ep == EstadoPago.PAGADO || ep == EstadoPago.A_CUENTA;
        c.setEstado(habiaCobro ? EstadoCompra.REEMBOLSADA : EstadoCompra.CANCELADA);
        c.setEstadoPago(habiaCobro ? EstadoPago.REEMBOLSADO : EstadoPago.CANCELADO);
        c.setMotivo(limpio(motivo));
        compraRepo.save(c);
        avisarCliente(c, habiaCobro ? "fue reembolsado" : "fue cancelado");
    }

    // ============================ COBRO EN EFECTIVO ============================

    @Transactional
    public void marcarCobrado(Long compraId, Long tiendaId) {
        Compra c = deLaTienda(compraId, tiendaId);
        if (c.getMetodoPago() != MetodoPago.EFECTIVO || c.getEstadoPago() != EstadoPago.PENDIENTE) {
            throw new IllegalArgumentException("Este pedido no tiene un cobro en efectivo pendiente.");
        }
        if (c.getEstado() == EstadoCompra.CANCELADA || c.getEstado() == EstadoCompra.REEMBOLSADA) {
            throw new IllegalArgumentException("El pedido está cancelado.");
        }
        c.setEstadoPago(EstadoPago.PAGADO);
        compraRepo.save(c);
    }

    // ============================== EDITAR PEDIDO ==============================

    @Transactional
    public void eliminarItem(Long compraId, Long itemId, Long tiendaId) {
        Compra compra = deLaTienda(compraId, tiendaId);
        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new IllegalArgumentException("Solo se editan pedidos pendientes.");
        }
        if (compra.getEstadoPago() == EstadoPago.PAGADO) {
            throw new IllegalArgumentException("Este pedido ya se cobró con tarjeta: cancélalo (se reembolsa) en vez de editarlo.");
        }
        CompraItem item = compra.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El producto no pertenece a este pedido."));
        if (compra.getItems().size() == 1) {
            throw new IllegalArgumentException("Es el único producto del pedido: mejor cancela el pedido.");
        }

        compra.getItems().remove(item); // orphanRemoval lo borra de la BD

        // Recalcular totales con el IVA de la tienda
        BigDecimal suma = compra.getItems().stream()
                .map(i -> i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ConfiguracionTienda cfg = configService.de(tiendaId);
        Totales t = Totales.de(suma, cfg.getIva(), cfg.isPreciosIncluyenIva());
        compra.setSubtotal(t.subtotal());
        compra.setImpuestos(t.impuestos());
        compra.setTotal(t.total());
        compraRepo.save(compra);
    }

    // ================================ helpers ================================

    private Compra deLaTienda(Long compraId, Long tiendaId) {
        Compra c = compraRepo.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe."));
        if (!c.getTienda().getId().equals(tiendaId)) {
            throw new IllegalArgumentException("El pedido no es de esta tienda.");
        }
        return c;
    }

    private void avisarCliente(Compra c, String que) {
        TipoNotificacion tipo = c.getEstado() == EstadoCompra.REEMBOLSADA
                ? TipoNotificacion.COMPRA_REEMBOLSADA : TipoNotificacion.COMPRA_CANCELADA;
        notificaciones.resolverReferencia(TipoNotificacion.COMPRA_NUEVA, c.getId());
        notificaciones.notificar(c.getUsuario(), c.getTienda(), tipo,
                "Tu pedido #" + c.getId() + " en " + c.getTienda().getNombre() + " " + que,
                c.getMotivo() == null ? null : "Motivo: " + c.getMotivo(),
                "/compras", c.getId());
    }

    private static String limpio(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() > 255 ? t.substring(0, 255) : t;
    }
}
