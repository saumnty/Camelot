package com.example.clientes_venta.Tienda;

import java.time.LocalDateTime;

import com.example.clientes_venta.Usuario.Usuario;

import jakarta.persistence.*;

@Entity
@Table(name = "tienda_solicitud_admin",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"tienda_id", "solicitante_id", "estado"})
       })
public class TiendaSolicitudAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tienda destino
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id")
    private Tienda tienda;

    // Usuario que solicita ser ADMIN
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id")
    private Usuario solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudAdmin estado = EstadoSolicitudAdmin.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    // Admin que aprobó/rechazó
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private Usuario resolvedBy;

    @Column(length = 300)
    private String nota;

    // ===== getters/setters =====

    public Long getId() { return id; }

    public Tienda getTienda() { return tienda; }
    public void setTienda(Tienda tienda) { this.tienda = tienda; }

    public Usuario getSolicitante() { return solicitante; }
    public void setSolicitante(Usuario solicitante) { this.solicitante = solicitante; }

    public EstadoSolicitudAdmin getEstado() { return estado; }
    public void setEstado(EstadoSolicitudAdmin estado) { this.estado = estado; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public Usuario getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(Usuario resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
}
