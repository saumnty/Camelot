package com.example.clientes_venta.Pago;

public enum MetodoPago {
    TARJETA,   // se cobra al hacer el pedido
    EFECTIVO,  // se paga al recoger
    FIADO;     // se carga a la cuenta del cliente frecuente cuando el admin confirma

    public String etiqueta() {
        return switch (this) {
            case TARJETA -> "Tarjeta";
            case EFECTIVO -> "Efectivo al recoger";
            case FIADO -> "Fiado";
        };
    }
}
