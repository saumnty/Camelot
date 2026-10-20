package com.example.clientes_venta.Producto.importar;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Una fila del archivo, ya interpretada. Se guarda en la sesión entre la vista previa y "Confirmar".
 */
public class FilaImportada implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Accion { NUEVO, ACTUALIZAR, SIN_CAMBIOS, ERROR }

    private int numero;              // número de fila como lo ve el usuario en Excel (encabezado = 1)
    private String codigo;
    private String nombre;
    private BigDecimal precio;
    private BigDecimal costo;
    private Integer stock;
    private String categoria;
    private String subcategoria;
    private String unidad;
    private Double cantidad;
    private String imagenUrl;

    private Accion accion = Accion.NUEVO;
    private Long productoId;              // si ACTUALIZAR
    private boolean completadoConCodigo;  // nombre/foto traídos de Open Food Facts
    private final List<String> errores = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();

    public boolean tieneErrores() { return !errores.isEmpty(); }
    public void error(String e) { errores.add(e); accion = Accion.ERROR; }
    public void aviso(String a) { avisos.add(a); }

    // ---- getters / setters (Thymeleaf los usa) ----
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getSubcategoria() { return subcategoria; }
    public void setSubcategoria(String subcategoria) { this.subcategoria = subcategoria; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public Double getCantidad() { return cantidad; }
    public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public Accion getAccion() { return accion; }
    public void setAccion(Accion accion) { this.accion = accion; }
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public boolean isCompletadoConCodigo() { return completadoConCodigo; }
    public void setCompletadoConCodigo(boolean v) { this.completadoConCodigo = v; }
    public List<String> getErrores() { return errores; }
    public List<String> getAvisos() { return avisos; }
}
