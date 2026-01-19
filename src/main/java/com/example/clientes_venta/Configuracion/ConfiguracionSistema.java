package com.example.clientes_venta.Configuracion;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "configuracion_sistema")
public class ConfiguracionSistema {

    @Id
    private Long id; // siempre 1

    @NotBlank(message = "El nombre del negocio es obligatorio.")
    @Size(max = 80, message = "Máximo 80 caracteres.")
    private String nombreNegocio;

    @Size(max = 13, message = "Máximo 13 caracteres.")
    private String rfc;

    @Size(max = 180, message = "Máximo 180 caracteres.")
    private String direccion;

    @Size(max = 30, message = "Máximo 30 caracteres.")
    private String telefono;

    @Email(message = "Email inválido.")
    @Size(max = 120, message = "Máximo 120 caracteres.")
    private String emailContacto;

    @NotNull(message = "El IVA es obligatorio.")
    @DecimalMin(value = "0.0", message = "El IVA no puede ser menor a 0.")
    @DecimalMax(value = "100.0", message = "El IVA no puede ser mayor a 100.")
    private Double iva;    // % de IVA, ej. 16.0

    @NotBlank(message = "La moneda es obligatoria.")
    @Size(max = 10, message = "Máximo 10 caracteres.")
    private String moneda; // MXN, USD, etc.

    // Checkbox: puede llegar null si no viene en el POST (según form).
    // Lo normalizamos en el service.
    private Boolean autoConfirmarSiHayStock;
}