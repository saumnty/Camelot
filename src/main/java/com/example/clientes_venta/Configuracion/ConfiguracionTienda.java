package com.example.clientes_venta.Configuracion;

import java.time.LocalDateTime;

import com.example.clientes_venta.Tienda.Tienda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuración PROPIA de cada tienda. Reemplaza a la vieja "configuracion_sistema"
 * (una sola fila para todas las tiendas: si un dueño cambiaba el IVA, cambiaba para todos).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "configuracion_tienda")
public class ConfiguracionTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false, unique = true)
    private Tienda tienda;

    // ---------- Datos fiscales y de contacto (salen en el ticket) ----------
    @Column(length = 13)
    private String rfc;

    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Column(length = 180)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Column(name = "email_contacto", length = 120)
    private String emailContacto;

    @Column(length = 120)
    private String horario;

    @Column(name = "mensaje_ticket", length = 200)
    private String mensajeTicket;

    // ---------- Impuestos ----------
    @Column(nullable = false)
    private Double iva = 16.0;

    // true: el precio de los productos ya trae IVA (lo normal en tiendita); el ticket solo lo desglosa.
    // false: el IVA se suma encima del precio.
    @Column(name = "precios_incluyen_iva", nullable = false)
    private boolean preciosIncluyenIva = true;

    @Column(nullable = false, length = 10)
    private String moneda = "MXN";

    // ---------- Ventas ----------
    @Column(name = "auto_confirmar_si_hay_stock", nullable = false)
    private boolean autoConfirmarSiHayStock = false;

    // null = no avisar
    @Column(name = "stock_bajo_umbral")
    private Integer stockBajoUmbral;

    // ---------- Formas de pago ----------
    @Column(name = "acepta_tarjeta", nullable = false)
    private boolean aceptaTarjeta = true;

    @Column(name = "acepta_efectivo", nullable = false)
    private boolean aceptaEfectivo = true;

    @Column(name = "acepta_fiado", nullable = false)
    private boolean aceptaFiado = false;

    @Column(name = "permite_reembolsos", nullable = false)
    private boolean permiteReembolsos = true;

    // ---------- Clientes ----------
    @Column(name = "permite_clientes_frecuentes", nullable = false)
    private boolean permiteClientesFrecuentes = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void tocar() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Al menos una forma de pago tiene que quedar activa. */
    public boolean tieneAlgunMetodoPago() {
        return aceptaTarjeta || aceptaEfectivo || aceptaFiado;
    }
}
