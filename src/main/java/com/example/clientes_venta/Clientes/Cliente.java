package com.example.clientes_venta.Clientes;

import java.math.BigDecimal;

import com.example.clientes_venta.Tienda.Tienda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    // ===== NUEVO: DEUDAS =====
    @Column(precision = 12, scale = 2)
    private BigDecimal deudaMonto; // null o 0 = no debe

    @Column(length = 255)
    private String deudaNota;

    // Cada tienda tiene su propia libreta. Antes la tabla era global y todos veían
    // los clientes de todos. Las filas viejas sin tienda ya no se muestran.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id")
    private Tienda tienda;

    public Tienda getTienda() { return tienda; }
    public void setTienda(Tienda tienda) { this.tienda = tienda; }

    // ===== Constructores =====
    public Cliente() {}

    public Cliente(String nombre, String email, String telefono) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
    }

    // ===== Getters y Setters =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public BigDecimal getDeudaMonto() {
        return deudaMonto;
    }

    public void setDeudaMonto(BigDecimal deudaMonto) {
        this.deudaMonto = deudaMonto;
    }

    public String getDeudaNota() {
        return deudaNota;
    }

    public void setDeudaNota(String deudaNota) {
        this.deudaNota = deudaNota;
    }
}