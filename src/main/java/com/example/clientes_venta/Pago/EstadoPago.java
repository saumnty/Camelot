package com.example.clientes_venta.Pago;

public enum EstadoPago {
    PENDIENTE,    // efectivo por cobrar / fiado sin autorizar
    PAGADO,       // tarjeta cobrada o efectivo recibido
    A_CUENTA,     // fiado autorizado: se sumó a lo que debe el cliente
    REEMBOLSADO,  // se devolvió el dinero (o se quitó de la cuenta de fiado)
    CANCELADO;    // pedido cancelado antes de cobrarse

    public String etiqueta() {
        return switch (this) {
            case PENDIENTE -> "Por cobrar";
            case PAGADO -> "Pagado";
            case A_CUENTA -> "A cuenta (fiado)";
            case REEMBOLSADO -> "Reembolsado";
            case CANCELADO -> "Sin cobro";
        };
    }
}
