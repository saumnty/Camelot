package com.example.clientes_venta.Tienda;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tienda_proveedor")
public class TiendaProveedor {

    @EmbeddedId
    private TiendaProveedorId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tiendaId")
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("proveedorId")
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Tienda proveedor;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public TiendaProveedor() {}

    public TiendaProveedor(Tienda tienda, Tienda proveedor) {
        this.tienda = tienda;
        this.proveedor = proveedor;
        this.id = new TiendaProveedorId(proveedor.getId(), tienda.getId()); // OJO orden
        this.activo = true;
        this.creadoEn = LocalDateTime.now();
    }

    public TiendaProveedorId getId() { return id; }
    public Tienda getTienda() { return tienda; }
    public Tienda getProveedor() { return proveedor; }
    public Boolean getActivo() { return activo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }

    public void setId(TiendaProveedorId id) { this.id = id; }
    public void setTienda(Tienda tienda) { this.tienda = tienda; }
    public void setProveedor(Tienda proveedor) { this.proveedor = proveedor; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
