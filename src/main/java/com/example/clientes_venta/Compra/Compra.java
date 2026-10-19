package com.example.clientes_venta.Compra;

import com.example.clientes_venta.Pago.EstadoPago;
import com.example.clientes_venta.Pago.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Usuario.Usuario;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "compra")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // comprador
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // tienda que vende (clave para "Mis ventas")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCompra estado;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CompraItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(length = 255)
    private String motivo;

    // ================= Pago e impuestos =================
    // Columnas NUEVAS y nullables a propósito: las compras viejas no las tienen
    // y Hibernate (ddl-auto=update) no puede agregar NOT NULL a una tabla con datos.

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", length = 20)
    private EstadoPago estadoPago;

    // total = subtotal + impuestos
    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal impuestos;

    /** Para compras viejas sin desglose. */
    public BigDecimal getSubtotalMostrado() {
        return subtotal != null ? subtotal : total;
    }

    public BigDecimal getImpuestosMostrados() {
        return impuestos != null ? impuestos : BigDecimal.ZERO;
    }
}
