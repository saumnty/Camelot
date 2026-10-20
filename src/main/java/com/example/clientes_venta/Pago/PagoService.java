package com.example.clientes_venta.Pago;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Compra.Compra;

/** Une las compras con la pasarela de pago (hoy, la simulada). */
@Service
public class PagoService {

    private final ProveedorPago proveedor;
    private final PagoRepository pagoRepo;

    public PagoService(ProveedorPago proveedor, PagoRepository pagoRepo) {
        this.proveedor = proveedor;
        this.pagoRepo = pagoRepo;
    }

    public boolean esSimulado() {
        return "SIMULADO".equals(proveedor.nombre());
    }

    public void validarTarjeta(ProveedorPago.DatosTarjeta datos) {
        proveedor.validar(datos);
    }

    /**
     * Cobra la compra con tarjeta. Si el banco la rechaza lanza PagoRechazadoException;
     * como se llama dentro de la transacción del checkout, no se crea ningún pedido.
     */
    @Transactional
    public Pago cobrarTarjeta(Compra compra, ProveedorPago.DatosTarjeta datos) {
        ProveedorPago.ResultadoCobro r = proveedor.cobrar(datos, compra.getTotal(),
                "Pedido #" + compra.getId() + " en " + compra.getTienda().getNombre());
        if (!r.aprobado()) {
            throw new PagoRechazadoException("Pago rechazado: " + r.mensaje());
        }
        Pago p = new Pago();
        p.setCompra(compra);
        p.setMetodo(MetodoPago.TARJETA);
        p.setEstado(EstadoPago.PAGADO);
        p.setMonto(compra.getTotal());
        p.setProveedor(proveedor.nombre());
        p.setReferencia(r.referencia());
        p.setMarcaTarjeta(r.marca());
        p.setUltimos4(r.ultimos4());
        p.setMensaje(r.mensaje());
        return pagoRepo.save(p);
    }

    /** Devuelve el dinero de una compra pagada con tarjeta. */
    @Transactional
    public void reembolsarTarjeta(Compra compra) {
        Pago p = pagoRepo.findFirstByCompra_IdAndMetodoOrderByCreatedAtDesc(compra.getId(), MetodoPago.TARJETA)
                .orElseThrow(() -> new IllegalStateException("No se encontró el cobro de la compra #" + compra.getId()));
        if (p.getEstado() == EstadoPago.REEMBOLSADO) return;

        ProveedorPago.ResultadoCobro r = proveedor.reembolsar(p.getReferencia(), p.getMonto());
        if (!r.aprobado()) {
            throw new IllegalStateException("La pasarela no aceptó el reembolso: " + r.mensaje());
        }
        p.setEstado(EstadoPago.REEMBOLSADO);
        p.setReembolsadoEn(LocalDateTime.now());
        p.setMensaje(r.mensaje());
        pagoRepo.save(p);
    }

    public Optional<Pago> cobroTarjeta(Long compraId) {
        return pagoRepo.findFirstByCompra_IdAndMetodoOrderByCreatedAtDesc(compraId, MetodoPago.TARJETA);
    }
}
