package com.example.clientes_venta.Tienda;

import java.time.OffsetDateTime;

import com.example.clientes_venta.Usuario.Usuario;
import jakarta.persistence.*;

@Entity
@Table(
    name = "tienda_usuario",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tienda_id", "usuario_id"})
)
public class TiendaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolTienda rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMembresia estado = EstadoMembresia.ACTIVO;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    // getters y setters
    public Long getId() { return id; }
    public Tienda getTienda() { return tienda; }
    public Usuario getUsuario() { return usuario; }
    public RolTienda getRol() { return rol; }
    public EstadoMembresia getEstado() { return estado; }

    public void setTienda(Tienda tienda) { this.tienda = tienda; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public void setRol(RolTienda rol) { this.rol = rol; }
    public void setEstado(EstadoMembresia estado) { this.estado = estado; }
}
