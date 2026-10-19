package com.example.clientes_venta.Pago;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pasarela FALSA para desarrollo. No mueve dinero.
 *
 * Tarjetas de prueba (cualquier titular, cualquier fecha futura, cualquier CVV):
 *   4242 4242 4242 4242   Visa        → aprobada
 *   5555 5555 5555 4444   Mastercard  → aprobada
 *   3782 822463 10005     Amex        → aprobada
 *   4000 0000 0000 0002   Visa        → rechazada (tarjeta declinada)
 *   4000 0000 0000 9995   Visa        → rechazada (fondos insuficientes)
 * Cualquier otro número válido (pasa la prueba de Luhn) se aprueba.
 */
@Component
@ConditionalOnProperty(name = "camelot.pagos.proveedor", havingValue = "simulado", matchIfMissing = true)
public class ProveedorPagoSimulado implements ProveedorPago {

    @Override
    public String nombre() {
        return "SIMULADO";
    }

    @Override
    public void validar(DatosTarjeta t) {
        if (t == null) throw new PagoRechazadoException("Faltan los datos de la tarjeta.");

        String numero = soloDigitos(t.numero());
        if (numero.length() < 13 || numero.length() > 19 || !luhn(numero)) {
            throw new PagoRechazadoException("El número de tarjeta no es válido.");
        }
        if (t.titular() == null || t.titular().isBlank()) {
            throw new PagoRechazadoException("Escribe el nombre como aparece en la tarjeta.");
        }
        String cvv = soloDigitos(t.cvv());
        int cvvLargo = marca(numero).equals("Amex") ? 4 : 3;
        if (cvv.length() != cvvLargo) {
            throw new PagoRechazadoException("El código de seguridad (CVV) debe tener " + cvvLargo + " dígitos.");
        }
        YearMonth venc = vencimiento(t.vencimiento());
        if (venc == null) {
            throw new PagoRechazadoException("La fecha de vencimiento debe ser MM/AA.");
        }
        if (venc.isBefore(YearMonth.now())) {
            throw new PagoRechazadoException("La tarjeta está vencida.");
        }
    }

    @Override
    public ResultadoCobro cobrar(DatosTarjeta t, BigDecimal monto, String descripcion) {
        validar(t);
        String numero = soloDigitos(t.numero());
        String marca = marca(numero);
        String ultimos4 = numero.substring(numero.length() - 4);

        if (numero.equals("4000000000000002")) {
            return new ResultadoCobro(false, null, marca, ultimos4, "Tarjeta declinada por el banco.");
        }
        if (numero.equals("4000000000009995")) {
            return new ResultadoCobro(false, null, marca, ultimos4, "Fondos insuficientes.");
        }
        String ref = "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return new ResultadoCobro(true, ref, marca, ultimos4, "Pago simulado aprobado.");
    }

    @Override
    public ResultadoCobro reembolsar(String referencia, BigDecimal monto) {
        String ref = "SIMR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return new ResultadoCobro(true, ref, null, null, "Reembolso simulado de " + referencia);
    }

    // --------------------------- utilidades ---------------------------

    static String soloDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /** Algoritmo de Luhn: el dígito verificador que traen todas las tarjetas. */
    static boolean luhn(String numero) {
        int suma = 0;
        boolean doblar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (doblar) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            suma += d;
            doblar = !doblar;
        }
        return suma % 10 == 0;
    }

    static String marca(String numero) {
        if (numero.startsWith("4")) return "Visa";
        if (numero.startsWith("34") || numero.startsWith("37")) return "Amex";
        if (numero.length() >= 2) {
            int dos = Integer.parseInt(numero.substring(0, 2));
            if (dos >= 51 && dos <= 55) return "Mastercard";
        }
        if (numero.length() >= 4) {
            int cuatro = Integer.parseInt(numero.substring(0, 4));
            if (cuatro >= 2221 && cuatro <= 2720) return "Mastercard";
        }
        return "Tarjeta";
    }

    /** "08/27" o "08/2027" → 2027-08 */
    static YearMonth vencimiento(String mmAa) {
        if (mmAa == null) return null;
        String[] p = mmAa.trim().split("\\s*/\\s*");
        if (p.length != 2) return null;
        try {
            int mes = Integer.parseInt(p[0]);
            int anio = Integer.parseInt(p[1]);
            if (anio < 100) anio += 2000;
            if (mes < 1 || mes > 12) return null;
            return YearMonth.of(anio, mes);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
