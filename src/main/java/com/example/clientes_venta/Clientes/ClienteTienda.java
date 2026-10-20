package com.example.clientes_venta.Clientes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.clientes_venta.Tienda.Tienda;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relación "cliente frecuente" entre un usuario y una tienda, con su cuenta de fiado.
 * El saldo es lo que el cliente DEBE a la tienda.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cliente_tienda",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tienda_id", "usuario_id"}))
public class ClienteTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoClienteTienda estado = EstadoClienteTienda.SOLICITADO;

    // ---- Fiado ----
    @Column(name = "fiado_permitido", nullable = false)
    private boolean fiadoPermitido = false;

    @Column(name = "limite_credito", precision = 12, scale = 2)
    private BigDecimal limiteCredito;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    // ---- Seguimiento ----
    @Column(name = "nota_solicitud", length = 300)
    private String notaSolicitud;

    @Column(name = "nota_admin", length = 300)
    private String notaAdmin;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resuelto_en")
    private LocalDateTime resueltoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelto_por_id")
    private Usuario resueltoPor;

    /** Cuánto más le pueden fiar ahora mismo. */
    public BigDecimal getDisponible() {
        if (!puedeFiar() || limiteCredito == null) return BigDecimal.ZERO;
        BigDecimal d = limiteCredito.subtract(saldo == null ? BigDecimal.ZERO : saldo);
        return d.signum() < 0 ? BigDecimal.ZERO : d;
    }

    public boolean puedeFiar() {
        return estado == EstadoClienteTienda.APROBADO && fiadoPermitido;
    }
}
