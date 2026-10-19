package com.example.clientes_venta.Pago;

/** Pago no aprobado. Al lanzarse dentro del checkout se deshace todo (no se crean pedidos). */
public class PagoRechazadoException extends RuntimeException {
    public PagoRechazadoException(String mensaje) {
        super(mensaje);
    }
}
