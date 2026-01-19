package com.example.clientes_venta.Producto;

import java.math.BigDecimal;

import com.example.clientes_venta.Categoria.Categoria;
import com.example.clientes_venta.Subcategoria.Subcategoria;
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
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    private BigDecimal precio;
    private Integer stock;

    // NUEVO: costo para calcular utilidad estimada (opcional)
    @Column(name = "costo_proveedor", precision = 12, scale = 2)
    private BigDecimal costoProveedor;

    @Column(name = "imagen_url", columnDefinition = "text")
    private String imagenUrl;

    @Column(name = "unidad_medida", columnDefinition = "text")
    private String unidadMedida;

    @Column(name = "cantidad_medida")
    private Double cantidadMedida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategoria_id")
    private Subcategoria subcategoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tienda_id")
    private Tienda tienda;

    @Column(nullable = false)
    private boolean destacado = false;

    public boolean isDestacado() { return destacado; }
    public void setDestacado(boolean destacado) { this.destacado = destacado; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public BigDecimal getCostoProveedor() { return costoProveedor; }
    public void setCostoProveedor(BigDecimal costoProveedor) { this.costoProveedor = costoProveedor; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public Double getCantidadMedida() { return cantidadMedida; }
    public void setCantidadMedida(Double cantidadMedida) { this.cantidadMedida = cantidadMedida; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public Subcategoria getSubcategoria() { return subcategoria; }
    public void setSubcategoria(Subcategoria subcategoria) { this.subcategoria = subcategoria; }

    public Tienda getTienda() { return tienda; }
    public void setTienda(Tienda tienda) { this.tienda = tienda; }
}