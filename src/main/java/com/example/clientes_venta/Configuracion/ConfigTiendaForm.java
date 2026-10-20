package com.example.clientes_venta.Configuracion;

import com.example.clientes_venta.Tienda.Tienda;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Lo que se edita en "Configuración de la tienda": datos de la tienda + su configuración. */
@Getter
@Setter
public class ConfigTiendaForm {

    // ---- Tienda (lo que ven los clientes) ----
    @NotBlank(message = "El nombre del negocio es obligatorio.")
    @Size(max = 120, message = "Nombre: máximo 120 caracteres.")
    private String nombre;

    @Size(max = 500, message = "Descripción: máximo 500 caracteres.")
    private String descripcion;

    @Size(max = 80, message = "Categoría: máximo 80 caracteres.")
    private String categoria;

    @Size(max = 80, message = "Ciudad: máximo 80 caracteres.")
    private String ciudad;

    @Size(max = 255, message = "Enlace de mapa: máximo 255 caracteres.")
    @Pattern(regexp = "^$|^https://.*", message = "El enlace de Google Maps debe empezar con https://")
    private String googleMapsUrl;

    private boolean activa = true;

    // ---- Datos fiscales / contacto ----
    @Size(max = 13, message = "RFC: máximo 13 caracteres.")
    @Pattern(regexp = "^$|^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$", message = "El RFC no tiene un formato válido.")
    private String rfc;

    @Size(max = 150, message = "Razón social: máximo 150 caracteres.")
    private String razonSocial;

    @Size(max = 180, message = "Dirección: máximo 180 caracteres.")
    private String direccion;

    @Size(max = 30, message = "Teléfono: máximo 30 caracteres.")
    private String telefono;

    @Email(message = "Correo de contacto inválido.")
    @Size(max = 120, message = "Correo: máximo 120 caracteres.")
    private String emailContacto;

    @Size(max = 120, message = "Horario: máximo 120 caracteres.")
    private String horario;

    @Size(max = 200, message = "Mensaje del ticket: máximo 200 caracteres.")
    private String mensajeTicket;

    // ---- Impuestos ----
    @NotNull(message = "El IVA es obligatorio.")
    @DecimalMin(value = "0.0", message = "El IVA no puede ser menor a 0.")
    @DecimalMax(value = "100.0", message = "El IVA no puede ser mayor a 100.")
    private Double iva = 16.0;

    private boolean preciosIncluyenIva = true;

    @NotBlank(message = "La moneda es obligatoria.")
    private String moneda = "MXN";

    // ---- Ventas ----
    private boolean autoConfirmarSiHayStock;

    @Min(value = 0, message = "El aviso de stock bajo no puede ser negativo.")
    private Integer stockBajoUmbral;

    // ---- Pagos ----
    private boolean aceptaTarjeta = true;
    private boolean aceptaEfectivo = true;
    private boolean aceptaFiado;
    private boolean permiteReembolsos = true;

    // ---- Clientes ----
    private boolean permiteClientesFrecuentes = true;

    // El RFC se guarda en mayúsculas aunque lo escriban en minúsculas
    public void setRfc(String rfc) {
        this.rfc = rfc == null ? null : rfc.trim().toUpperCase();
    }

    public static ConfigTiendaForm desde(Tienda t, ConfiguracionTienda c) {
        ConfigTiendaForm f = new ConfigTiendaForm();
        f.nombre = t.getNombre();
        f.descripcion = t.getDescripcion();
        f.categoria = t.getCategoria();
        f.ciudad = t.getCiudad();
        f.googleMapsUrl = t.getGoogleMapsUrl();
        f.activa = !Boolean.FALSE.equals(t.getActiva());
        f.rfc = c.getRfc();
        f.razonSocial = c.getRazonSocial();
        f.direccion = c.getDireccion();
        f.telefono = c.getTelefono();
        f.emailContacto = c.getEmailContacto();
        f.horario = c.getHorario();
        f.mensajeTicket = c.getMensajeTicket();
        f.iva = c.getIva();
        f.preciosIncluyenIva = c.isPreciosIncluyenIva();
        f.moneda = c.getMoneda();
        f.autoConfirmarSiHayStock = c.isAutoConfirmarSiHayStock();
        f.stockBajoUmbral = c.getStockBajoUmbral();
        f.aceptaTarjeta = c.isAceptaTarjeta();
        f.aceptaEfectivo = c.isAceptaEfectivo();
        f.aceptaFiado = c.isAceptaFiado();
        f.permiteReembolsos = c.isPermiteReembolsos();
        f.permiteClientesFrecuentes = c.isPermiteClientesFrecuentes();
        return f;
    }

    public void aplicarA(ConfiguracionTienda c) {
        c.setRfc(limpio(rfc == null ? null : rfc.toUpperCase()));
        c.setRazonSocial(limpio(razonSocial));
        c.setDireccion(limpio(direccion));
        c.setTelefono(limpio(telefono));
        c.setEmailContacto(limpio(emailContacto));
        c.setHorario(limpio(horario));
        c.setMensajeTicket(limpio(mensajeTicket));
        c.setIva(iva);
        c.setPreciosIncluyenIva(preciosIncluyenIva);
        c.setMoneda(moneda.trim());
        c.setAutoConfirmarSiHayStock(autoConfirmarSiHayStock);
        c.setStockBajoUmbral(stockBajoUmbral);
        c.setAceptaTarjeta(aceptaTarjeta);
        c.setAceptaEfectivo(aceptaEfectivo);
        c.setAceptaFiado(aceptaFiado);
        c.setPermiteReembolsos(permiteReembolsos);
        c.setPermiteClientesFrecuentes(permiteClientesFrecuentes);
    }

    static String limpio(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
