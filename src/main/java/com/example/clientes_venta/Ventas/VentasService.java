package com.example.clientes_venta.Ventas;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Compra.Compra;
import com.example.clientes_venta.Compra.CompraItem;
import com.example.clientes_venta.Compra.CompraRepository;
import com.example.clientes_venta.Compra.CompraItemRepository;
import com.example.clientes_venta.Compra.EstadoCompra;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaRepository;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Service
public class VentasService {

    private final CompraRepository compraRepo;
    private final CompraItemRepository itemRepo;
    private final TiendaRepository tiendaRepo;
    private final UsuarioRepo usuarioRepo;

    public VentasService(CompraRepository compraRepo,
                         CompraItemRepository itemRepo,
                         TiendaRepository tiendaRepo,
                         UsuarioRepo usuarioRepo) {
        this.compraRepo = compraRepo;
        this.itemRepo = itemRepo;
        this.tiendaRepo = tiendaRepo;
        this.usuarioRepo = usuarioRepo;
    }

    // ✅ Mis ventas = compras pendientes de mi tienda
    public List<Compra> ventasPendientesPorTienda(Long tiendaId) {
        return compraRepo.findByTienda_IdAndEstadoOrderByFechaDesc(tiendaId, EstadoCompra.PENDIENTE);
    }

    // ✅ Aprobar = cambiar estado (stock lo metemos después)
    @Transactional
    public void aprobarVenta(Long compraId) {
        Compra c = compraRepo.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no existe"));

        if (c.getEstado() != EstadoCompra.PENDIENTE) return;

        c.setEstado(EstadoCompra.CONFIRMADA);
        compraRepo.save(c);
    }

    // ✅ Cancelar = cambiar estado
    public void cancelarVenta(Long compraId, String motivo) {
        Compra compra = compraRepo.findById(compraId)
            .orElseThrow(() -> new IllegalArgumentException("Compra no existe"));

        // si ya no está pendiente, opcionalmente no permitir
        if (compra.getEstado() != EstadoCompra.PENDIENTE) return;

        compra.setEstado(EstadoCompra.CANCELADA);

        // guarda motivo solo si viene texto
        if (motivo != null && !motivo.trim().isEmpty()) {
            compra.setMotivo(motivo.trim());
        } else {
            compra.setMotivo(null);
        }

        compraRepo.save(compra);
    }


    @Transactional
    public void eliminarItem(Long compraId, Long itemId, String emailAdmin) {

        // (opcional pero recomendado) Validar que el admin sea dueño de la tienda de esa compra
        Usuario admin = usuarioRepo.findByEmail(emailAdmin)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        Tienda tienda = tiendaRepo.findFirstByOwner_Id(admin.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tienes tienda asignada"));

        Compra compra = compraRepo.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no existe"));

        if (!compra.getTienda().getId().equals(tienda.getId())) {
            throw new IllegalArgumentException("No puedes modificar ventas de otra tienda");
        }

        CompraItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item no existe"));

        if (!item.getCompra().getId().equals(compra.getId())) {
            throw new IllegalArgumentException("Item no pertenece a esta compra");
        }

        compra.getItems().remove(item);

        // ✅ recalcular total
        BigDecimal nuevoTotal = compra.getItems().stream()
            .map(i -> i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        compra.setTotal(nuevoTotal);

        // 👇 como tienes orphanRemoval=true, con quitarlo de la lista se borra
        compra.getItems().remove(item);
        compraRepo.save(compra);
    }

    public List<Compra> ventasPorTienda(Long tiendaId){
        return compraRepo.findByTienda_IdOrderByFechaDesc(tiendaId);
    }

}
