package com.example.clientes_venta.CompraExterna;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Usuario.Usuario;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "compra_externa")
public class CompraExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    // auditoría (opcional pero útil)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 140)
    private String concepto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(length = 255)
    private String nota;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}