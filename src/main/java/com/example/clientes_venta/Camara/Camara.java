package com.example.clientes_venta.Camara;

import java.time.LocalDateTime;

import com.example.clientes_venta.Tienda.Tienda;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "camara")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Camara {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tienda_id")
    private Tienda tienda;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String tipo = "RTSP";

    @Column(name = "rtsp_url", nullable = false, columnDefinition = "TEXT")
    private String rtspUrl;

    private String usuario;
    private String password;

    private String ubicacion;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}