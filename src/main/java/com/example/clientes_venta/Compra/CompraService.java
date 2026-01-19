package com.example.clientes_venta.Compra;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Producto.Producto;

@Service
public class CompraService {

    private final CompraRepository compraRepository;

    public CompraService(CompraRepository compraRepository) {
        this.compraRepository = compraRepository;
    }

    public List<Compra> misCompras(Integer usuarioId) {
        return compraRepository.findByUsuario_IdOrderByFechaDesc(usuarioId);
    }

    public List<Compra> ventasDeTienda(Long tiendaId) {
        return compraRepository.findByTienda_IdOrderByFechaDesc(tiendaId);
    }

    @Transactional
    public void aprobarCompra(Long compraId) {

        Compra compra = compraRepository.findById(compraId)
            .orElseThrow(() -> new IllegalArgumentException("Compra no existe"));

        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new IllegalArgumentException("La compra no está pendiente");
        }

        // 1) validar stock (aquí sí bloquea)
        for (CompraItem it : compra.getItems()) {
            Producto p = it.getProducto();
            Integer stock = p.getStock();

            if (stock != null && stock < it.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente para aprobar: " + p.getName());
            }
        }

        // 2) descontar stock (solo si no es NULL)
        for (CompraItem it : compra.getItems()) {
            Producto p = it.getProducto();
            Integer stock = p.getStock();

            if (stock != null) {
                p.setStock(stock - it.getCantidad());
            }
        }

        // 3) cambiar estado
        compra.setEstado(EstadoCompra.CONFIRMADA); // o COMPLETADA
        compraRepository.save(compra);
    }

    public Optional<Compra> buscarPorId(Long id) {
        return compraRepository.findById(id);
    }

}