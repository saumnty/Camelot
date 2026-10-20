package com.example.clientes_venta.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.clientes_venta.Compra.Compra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de un cobro con tarjeta. NUNCA se guarda el número completo ni el CVV:
 * solo la marca y los últimos 4 dígitos (igual que hacen las pasarelas reales).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    // "SIMULADO", "MERCADOPAGO", "STRIPE"...
    @Column(nullable = false, length = 30)
    private String proveedor;

    // Id de la operación en el proveedor (para reembolsar)
    @Column(length = 80)
    private String referencia;

    @Column(name = "marca_tarjeta", length = 20)
    private String marcaTarjeta;

    @Column(length = 4)
    private String ultimos4;

    @Column(length = 255)
    private String mensaje;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "reembolsado_en")
    private LocalDateTime reembolsadoEn;
}
