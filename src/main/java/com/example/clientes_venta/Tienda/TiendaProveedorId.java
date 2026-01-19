package com.example.clientes_venta.Tienda;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TiendaProveedorId implements Serializable {

    @Column(name = "proveedor_id")
    private Long proveedorId;

    @Column(name = "tienda_id")
    private Long tiendaId;

    public TiendaProveedorId() {}

    public TiendaProveedorId(Long proveedorId, Long tiendaId) {
        this.proveedorId = proveedorId;
        this.tiendaId = tiendaId;
    }

    public Long getProveedorId() { return proveedorId; }
    public Long getTiendaId() { return tiendaId; }

    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }
    public void setTiendaId(Long tiendaId) { this.tiendaId = tiendaId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TiendaProveedorId)) return false;
        TiendaProveedorId that = (TiendaProveedorId) o;
        return Objects.equals(proveedorId, that.proveedorId) &&
               Objects.equals(tiendaId, that.tiendaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proveedorId, tiendaId);
    }
}