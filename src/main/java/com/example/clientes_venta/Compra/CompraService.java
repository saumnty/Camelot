package com.example.clientes_venta.Compra;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

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

    public Optional<Compra> buscarPorId(Long id) {
        return compraRepository.findById(id);
    }

}