package com.example.clientes_venta.Clientes;

public enum EstadoClienteTienda {
    SOLICITADO,  // el cliente pidió ser frecuente; espera al admin
    APROBADO,    // cliente frecuente (puede tener fiado si el admin lo activó)
    RECHAZADO,
    SUSPENDIDO   // el admin le quitó el fiado/beneficios
}
