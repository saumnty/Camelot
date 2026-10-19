package com.example.clientes_venta.Compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Carrito.Carrito;
import com.example.clientes_venta.Carrito.CarritoItem;
import com.example.clientes_venta.Carrito.CarritoRepository;
import com.example.clientes_venta.Clientes.ClienteFrecuenteService;
import com.example.clientes_venta.Configuracion.ConfiguracionTienda;
import com.example.clientes_venta.Configuracion.ConfiguracionTiendaService;
import com.example.clientes_venta.Notificacion.NotificacionService;
import com.example.clientes_venta.Notificacion.TipoNotificacion;
import com.example.clientes_venta.Pago.EstadoPago;
import com.example.clientes_venta.Pago.MetodoPago;
import com.example.clientes_venta.Pago.PagoService;
import com.example.clientes_venta.Pago.ProveedorPago;
import com.example.clientes_venta.Pago.Totales;
import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Usuario.Usuario;

/**
 * Carrito → pedidos. Un carrito puede tener productos de varias tiendas: se crea un pedido
 * (Compra) por tienda, y cada tienda puede pagarse con un método distinto.
 *
 * Todo pasa en UNA transacción: si una tarjeta es rechazada, no se crea ningún pedido
 * y el carrito queda igual.
 */
@Service
public class CheckoutService {

    /** Lo que muestra la página de pago para cada tienda del carrito. */
    public record GrupoTienda(Tienda tienda,
                              List<CarritoItem> items,
                              Totales totales,
                              ConfiguracionTienda config,
                              Set<MetodoPago> metodos,
                              BigDecimal fiadoDisponible) {
        // Getters estilo JavaBean para Thymeleaf (g.tienda, g.items, ...)
        public Tienda getTienda() { return tienda; }
        public List<CarritoItem> getItems() { return items; }
        public Totales getTotales() { return totales; }
        public ConfiguracionTienda getConfig() { return config; }
        public Set<MetodoPago> getMetodos() { return metodos; }
        public BigDecimal getFiadoDisponible() { return fiadoDisponible; }
        public boolean acepta(String metodo) { return metodos.contains(MetodoPago.valueOf(metodo)); }
    }

    private final CarritoRepository carritoRepository;
    private final CompraRepository compraRepository;
    private final ConfiguracionTiendaService configService;
    private final ClienteFrecuenteService frecuentes;
    private final PagoService pagoService;
    private final InventarioService inventario;
    private final NotificacionService notificaciones;

    public CheckoutService(CarritoRepository carritoRepository,
                           CompraRepository compraRepository,
                           ConfiguracionTiendaService configService,
                           ClienteFrecuenteService frecuentes,
                           PagoService pagoService,
                           InventarioService inventario,
                           NotificacionService notificaciones) {
        this.carritoRepository = carritoRepository;
        this.compraRepository = compraRepository;
        this.configService = configService;
        this.frecuentes = frecuentes;
        this.pagoService = pagoService;
        this.inventario = inventario;
        this.notificaciones = notificaciones;
    }

    /** Agrupa el carrito por tienda y calcula totales + métodos de pago permitidos. */
    @Transactional(readOnly = true)
    public List<GrupoTienda> resumen(Usuario usuario) {
        Carrito carrito = carritoRepository.findByUsuario_IdAndActivoTrue(usuario.getId()).orElse(null);
        if (carrito == null || carrito.getItems() == null || carrito.getItems().isEmpty()) return List.of();

        Map<Long, List<CarritoItem>> porTienda = new LinkedHashMap<>();
        carrito.getItems().stream()
                .sorted(Comparator.comparing(it -> it.getProducto().getTienda().getNombre()))
                .forEach(it -> porTienda.computeIfAbsent(it.getProducto().getTienda().getId(), k -> new ArrayList<>()).add(it));

        List<GrupoTienda> grupos = new ArrayList<>();
        for (List<CarritoItem> items : porTienda.values()) {
            Tienda t = items.get(0).getProducto().getTienda();
            ConfiguracionTienda cfg = configService.de(t.getId());
            Totales tot = Totales.de(sumar(items), cfg.getIva(), cfg.isPreciosIncluyenIva());

            Set<MetodoPago> metodos = EnumSet.noneOf(MetodoPago.class);
            if (cfg.isAceptaTarjeta()) metodos.add(MetodoPago.TARJETA);
            if (cfg.isAceptaEfectivo()) metodos.add(MetodoPago.EFECTIVO);

            BigDecimal disponible = frecuentes.de(t.getId(), usuario)
                    .map(ct -> ct.getDisponible())
                    .orElse(BigDecimal.ZERO);
            if (cfg.isAceptaFiado() && disponible.compareTo(tot.total()) >= 0) metodos.add(MetodoPago.FIADO);

            grupos.add(new GrupoTienda(t, items, tot, cfg, metodos, disponible));
        }
        return grupos;
    }

    /**
     * Crea los pedidos.
     * @param metodos   método elegido por tienda (tiendaId → método)
     * @param tarjeta   datos de tarjeta (solo si alguna tienda se paga con TARJETA)
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Compra> checkout(Usuario usuario, Map<Long, MetodoPago> metodos, ProveedorPago.DatosTarjeta tarjeta) {

        List<GrupoTienda> grupos = resumen(usuario);
        if (grupos.isEmpty()) throw new IllegalArgumentException("Tu carrito está vacío.");

        // 1) Validar TODO antes de crear nada
        boolean hayTarjeta = false;
        for (GrupoTienda g : grupos) {
            MetodoPago m = metodos.get(g.tienda().getId());
            if (m == null) {
                throw new IllegalArgumentException("Elige cómo pagar en " + g.tienda().getNombre() + ".");
            }
            if (!g.metodos().contains(m)) {
                throw new IllegalArgumentException(g.tienda().getNombre() + " no acepta " + m.etiqueta().toLowerCase()
                        + (m == MetodoPago.FIADO ? " para este monto." : "."));
            }
            if (g.totales().total().signum() <= 0 && m == MetodoPago.TARJETA) {
                throw new IllegalArgumentException("El pedido de " + g.tienda().getNombre() + " está en $0; elige efectivo.");
            }
            hayTarjeta |= (m == MetodoPago.TARJETA);
        }
        if (hayTarjeta) pagoService.validarTarjeta(tarjeta); // lanza PagoRechazadoException

        // 2) Crear un pedido por tienda
        List<Compra> creadas = new ArrayList<>();
        for (GrupoTienda g : grupos) {
            MetodoPago metodo = metodos.get(g.tienda().getId());

            Compra compra = new Compra();
            compra.setUsuario(usuario);
            compra.setTienda(g.tienda());
            compra.setFecha(LocalDateTime.now());
            compra.setEstado(EstadoCompra.PENDIENTE);
            compra.setMetodoPago(metodo);
            compra.setEstadoPago(EstadoPago.PENDIENTE);
            compra.setSubtotal(g.totales().subtotal());
            compra.setImpuestos(g.totales().impuestos());
            compra.setTotal(g.totales().total());

            for (CarritoItem it : g.items()) {
                Producto p = it.getProducto();
                CompraItem ci = new CompraItem();
                ci.setCompra(compra);
                ci.setProducto(p);
                ci.setCantidad(it.getCantidad());
                ci.setPrecioUnitario(p.getPrecio() == null ? BigDecimal.ZERO : p.getPrecio()); // foto del precio
                compra.getItems().add(ci);
            }
            compra = compraRepository.save(compra);

            // 3) Cobro con tarjeta (si la rechazan, se deshace todo)
            if (metodo == MetodoPago.TARJETA) {
                pagoService.cobrarTarjeta(compra, tarjeta);
                compra.setEstadoPago(EstadoPago.PAGADO);
            }

            // 4) Auto-confirmación (nunca para fiado: ese lo autoriza el admin en persona)
            if (g.config().isAutoConfirmarSiHayStock() && metodo != MetodoPago.FIADO
                    && inventario.alcanzaParaAutoConfirmar(compra)) {
                inventario.descontar(compra);
                compra.setEstado(EstadoCompra.CONFIRMADA);
            }
            compra = compraRepository.save(compra);
            creadas.add(compra);

            avisar(compra);
        }

        // 5) Vaciar carrito
        Carrito carrito = carritoRepository.findByUsuario_IdAndActivoTrue(usuario.getId()).orElseThrow();
        carrito.setActivo(false);
        carrito.getItems().clear();
        carritoRepository.save(carrito);

        return creadas;
    }

    private void avisar(Compra c) {
        String quien = c.getUsuario().getName();
        String total = "$" + c.getTotal();
        if (c.getEstado() == EstadoCompra.CONFIRMADA) {
            notificaciones.notificarAdmins(c.getTienda(), TipoNotificacion.COMPRA_NUEVA,
                    "Venta confirmada automáticamente (#" + c.getId() + ")",
                    quien + " · " + total + " · " + c.getMetodoPago().etiqueta(),
                    "/ventas/historial?tiendaId=" + c.getTienda().getId(), c.getId());
            notificaciones.notificar(c.getUsuario(), c.getTienda(), TipoNotificacion.COMPRA_CONFIRMADA,
                    "Tu pedido #" + c.getId() + " en " + c.getTienda().getNombre() + " está confirmado",
                    total, "/compras/ticket/" + c.getId(), c.getId());
        } else {
            notificaciones.notificarAdmins(c.getTienda(), TipoNotificacion.COMPRA_NUEVA,
                    "Pedido nuevo por revisar (#" + c.getId() + ")",
                    quien + " · " + total + " · " + c.getMetodoPago().etiqueta(),
                    "/ventas?tiendaId=" + c.getTienda().getId(), c.getId());
        }
    }

    private static BigDecimal sumar(List<CarritoItem> items) {
        BigDecimal suma = BigDecimal.ZERO;
        for (CarritoItem it : items) {
            BigDecimal precio = it.getProducto().getPrecio() == null ? BigDecimal.ZERO : it.getProducto().getPrecio();
            suma = suma.add(precio.multiply(BigDecimal.valueOf(it.getCantidad())));
        }
        return suma;
    }
}
