package com.example.clientes_venta.Notificacion;

public enum TipoNotificacion {
    SOLICITUD_ADMIN,              // alguien dice "soy dueño" de una tienda
    SOLICITUD_ADMIN_RESUELTA,     // al solicitante: aprobada o rechazada
    SOLICITUD_FRECUENTE,          // un cliente pide ser cliente frecuente
    SOLICITUD_FRECUENTE_RESUELTA, // al cliente: aprobada o rechazada
    COMPRA_NUEVA,                 // a los admins: entró un pedido
    COMPRA_CONFIRMADA,            // al cliente
    COMPRA_CANCELADA,             // al cliente
    COMPRA_REEMBOLSADA,           // al cliente
    STOCK_BAJO,                   // a los admins
    FIADO_MOVIMIENTO,             // al cliente: cargo o abono en su cuenta
    GENERAL
}
