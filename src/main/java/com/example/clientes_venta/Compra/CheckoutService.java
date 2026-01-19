package com.example.clientes_venta.Compra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Carrito.Carrito;
import com.example.clientes_venta.Carrito.CarritoItem;
import com.example.clientes_venta.Carrito.CarritoRepository;
import com.example.clientes_venta.Configuracion.ConfiguracionSistema;
import com.example.clientes_venta.Configuracion.ConfiguracionSistemaRepo;
import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Producto.ProductoRepository;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Usuario.Usuario;


@Service
public class CheckoutService {

    private final CarritoRepository carritoRepository;
    private final CompraRepository compraRepository;
    private final ConfiguracionSistemaRepo configRepo;
    private final ProductoRepository productoRepository;

    public CheckoutService(
        CarritoRepository carritoRepository,
        CompraRepository compraRepository,
        ConfiguracionSistemaRepo configRepo,
        ProductoRepository productoRepository
    ) {
        this.carritoRepository = carritoRepository;
        this.compraRepository = compraRepository;
        this.configRepo = configRepo;
        this.productoRepository = productoRepository;
    }

    private BigDecimal calcularTotal(List<CompraItem> items) {
        return items.stream()
                .map(ci -> ci.getPrecioUnitario().multiply(BigDecimal.valueOf(ci.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public List<Compra> checkout(Usuario usuario) {

        Carrito carrito = carritoRepository.findByUsuario_IdAndActivoTrue(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("No hay carrito activo"));

        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new IllegalArgumentException("Carrito vacío");
        }

        // leer config global (id=1)
        boolean autoConfirmar = autoConfirmarHabilitado();

        // Agrupar por tienda
        Map<Tienda, List<CarritoItem>> porTienda = carrito.getItems().stream()
                .collect(Collectors.groupingBy(it -> it.getProducto().getTienda()));

        List<Compra> creadas = porTienda.entrySet().stream().map(entry -> {
            Tienda tienda = entry.getKey();
            List<CarritoItem> items = entry.getValue();

            Compra compra = new Compra();
            compra.setUsuario(usuario);
            compra.setTienda(tienda);
            compra.setFecha(LocalDateTime.now());
            compra.setEstado(EstadoCompra.PENDIENTE); // default

            // construir items (snapshot)
            for (CarritoItem it : items) {
                Producto p = it.getProducto();

                CompraItem ci = new CompraItem();
                ci.setCompra(compra);
                ci.setProducto(p);
                ci.setCantidad(it.getCantidad());
                ci.setPrecioUnitario(p.getPrecio() == null ? BigDecimal.ZERO : p.getPrecio());
                compra.getItems().add(ci);
            }

            compra.setTotal(calcularTotal(compra.getItems()));

            // ✅ Auto-confirmación: solo si está habilitado
            if (autoConfirmar) {

                boolean sePuedeAutoConfirmar = true;

                // 1) Validar stock con lock
                for (CompraItem it : compra.getItems()) {
                    Producto pLock = productoRepository.findByIdForUpdate(it.getProducto().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Producto no existe: " + it.getProducto().getId()));

                    Integer stock = pLock.getStock();

                    // regla: si stock es NULL => NO auto-confirmar
                    if (stock == null) { sePuedeAutoConfirmar = false; break; }

                    // si no alcanza => NO auto-confirmar
                    if (stock < it.getCantidad()) { sePuedeAutoConfirmar = false; break; }
                }

                // 2) Si todo bien, descontar stock y confirmar
                if (sePuedeAutoConfirmar) {
                    for (CompraItem it : compra.getItems()) {
                        Producto pLock = productoRepository.findByIdForUpdate(it.getProducto().getId()).get();
                        pLock.setStock(pLock.getStock() - it.getCantidad());
                        productoRepository.save(pLock);
                    }
                    compra.setEstado(EstadoCompra.CONFIRMADA);
                }
            }

            return compraRepository.save(compra);
        }).toList();

        // cerrar carrito
        carrito.setActivo(false);
        carrito.getItems().clear();
        carritoRepository.save(carrito);

        return creadas;
    }


    private boolean autoConfirmarHabilitado() {
        ConfiguracionSistema cfg = configRepo.findById(1L).orElse(null);
        return cfg != null && Boolean.TRUE.equals(cfg.getAutoConfirmarSiHayStock());
    }

}
