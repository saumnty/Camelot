package com.example.clientes_venta.Clientes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.clientes_venta.Usuario.Usuario;

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

/** Historial de la cuenta de fiado: cada compra fiada (CARGO) y cada pago del cliente (ABONO). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "movimiento_fiado")
public class MovimientoFiado {

    public enum Tipo { CARGO, ABONO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_tienda_id", nullable = false)
    private ClienteTienda clienteTienda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    // Compra que originó el cargo (si aplica)
    @Column(name = "compra_id")
    private Long compraId;

    @Column(length = 255)
    private String nota;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id")
    private Usuario registradoPor;
}
