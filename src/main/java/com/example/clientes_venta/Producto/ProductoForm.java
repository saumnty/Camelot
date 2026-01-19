package com.example.clientes_venta.Producto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductoForm {

    private Long id;

    private String name;
    private BigDecimal precio;   // ✅ BigDecimal, no Double
    private Integer stock;
    private String imagenUrl;

    private String unidadMedida;
    private Double cantidadMedida;

    // selección existente
    private Long categoriaId;
    private Long subcategoriaId;

    // creación nueva
    private String nuevaCategoria;
    private String nuevaSubcategoria;
}
