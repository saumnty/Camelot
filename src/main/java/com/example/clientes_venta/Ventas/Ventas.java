package com.example.clientes_venta.Ventas;

import java.time.LocalDateTime;

import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Usuario.Usuario;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="ventas")
public class Ventas {

    @Id
    private Integer id; // ✅ porque en BD es integer y NO es identity

    private Integer cantidad;

    // ✅ porque en BD es varchar(255)
    private String fecha;

    // ✅ porque en BD es smallint (ordinal 0..2)
    // NO pongas @Enumerated(EnumType.STRING)
    // ordinal es el default, pero lo dejo explícito para evitar confusión
    @Enumerated(EnumType.ORDINAL)
    private Estado estado;

    // ✅ cliente
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // ✅ tienda
    @ManyToOne
    @JoinColumn(name = "tienda_id")
    private Tienda tienda;

    // ✅ admin que valida
    @ManyToOne
    @JoinColumn(name = "validado_por_id")
    private Usuario validadoPor;

    private LocalDateTime fechaValidacion;

    private String motivoCancelacion;
}