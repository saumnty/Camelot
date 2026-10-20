package com.example.clientes_venta.Producto.importar;

/** Datos de un producto encontrados por su código de barras. */
public record ProductoExterno(
        String codigo,
        String nombre,
        String marca,
        Double cantidad,
        String unidad,
        String imagenUrl,
        String fuente        // "camelot" (ya existe en otra tienda) u "openfoodfacts"
) {}
