package com.example.clientes_venta.Compra;

import java.math.BigDecimal;

import com.example.clientes_venta.Producto.Producto;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "compra_item")
public class CompraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // pertenece a una compra
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    // producto comprado
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    // snapshot del precio al momento de comprar (para que si cambia el precio, tu compra no cambie)
    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal precioUnitario;

}
