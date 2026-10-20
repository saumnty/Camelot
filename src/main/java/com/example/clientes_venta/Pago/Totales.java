package com.example.clientes_venta.Pago;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Desglose subtotal / IVA / total de un pedido según la configuración de la tienda.
 *
 * Ejemplo con IVA 16% y precios de $116:
 *   precios incluyen IVA  → subtotal 100.00, IVA 16.00, total 116.00
 *   precios SIN IVA       → subtotal 116.00, IVA 18.56, total 134.56
 */
public record Totales(BigDecimal subtotal, BigDecimal impuestos, BigDecimal total) {

    // Getters estilo JavaBean para que Thymeleaf/SpEL los lea (g.totales.total)
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getImpuestos() { return impuestos; }
    public BigDecimal getTotal() { return total; }

    public static Totales de(BigDecimal sumaPrecios, Double ivaPorcentaje, boolean preciosIncluyenIva) {
        BigDecimal suma = (sumaPrecios == null ? BigDecimal.ZERO : sumaPrecios).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tasa = BigDecimal.valueOf(ivaPorcentaje == null ? 0.0 : ivaPorcentaje)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        if (tasa.signum() == 0) {
            return new Totales(suma, BigDecimal.ZERO.setScale(2), suma);
        }
        if (preciosIncluyenIva) {
            BigDecimal subtotal = suma.divide(BigDecimal.ONE.add(tasa), 2, RoundingMode.HALF_UP);
            return new Totales(subtotal, suma.subtract(subtotal), suma);
        }
        BigDecimal iva = suma.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        return new Totales(suma, iva, suma.add(iva));
    }
}
