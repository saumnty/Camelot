package com.example.clientes_venta.Tienda;

import java.time.LocalDateTime;

import com.example.clientes_venta.Usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


@Entity
@Table(name = "tienda")
public class Tienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 120)
    private String slug;

    @Column(nullable = false)
    private Boolean activa = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTienda tipo = TipoTienda.NORMAL;

    public TipoTienda getTipo() { return tipo; }
    public void setTipo(TipoTienda tipo) { this.tipo = tipo; }

    @Column(name = "fecha_de_creacion")
    private LocalDateTime fechaDeCreacion;

    @Column(length = 80)
    private String categoria;

    @Column(length = 80)
    private String ciudad;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "google_maps_url")
    private String googleMapsUrl;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Usuario owner;

    public Usuario getOwner() { return owner; }
    public void setOwner(Usuario owner) { this.owner = owner; }

    public Tienda() {}

    // getters/setters (puedes generar con tu IDE)
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getSlug() { return slug; }
    public Boolean getActiva() { return activa; }
    public LocalDateTime getFechaDeCreacion() { return fechaDeCreacion; }
    public String getCategoria() { return categoria; }
    public String getCiudad() { return ciudad; }
    public String getDescripcion() { return descripcion; }
    public String getGoogleMapsUrl() { return googleMapsUrl; }

    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setSlug(String slug) { this.slug = slug; }
    public void setActiva(Boolean activa) { this.activa = activa; }
    public void setFechaDeCreacion(LocalDateTime fechaDeCreacion) { this.fechaDeCreacion = fechaDeCreacion; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setGoogleMapsUrl(String googleMapsUrl) { this.googleMapsUrl = googleMapsUrl; }

}