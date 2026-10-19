package com.example.clientes_venta.Pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** Pruebas rápidas sin base de datos: IVA y pasarela simulada. */
class PagoSimuladoTest {

    private final ProveedorPagoSimulado pasarela = new ProveedorPagoSimulado();

    private static ProveedorPago.DatosTarjeta tarjeta(String numero, String venc, String cvv) {
        return new ProveedorPago.DatosTarjeta(numero, "Ana López", venc, cvv, null);
    }

    @Test
    void ivaIncluidoSoloSeDesglosa() {
        Totales t = Totales.de(new BigDecimal("116"), 16.0, true);
        assertEquals(new BigDecimal("100.00"), t.subtotal());
        assertEquals(new BigDecimal("16.00"), t.impuestos());
        assertEquals(new BigDecimal("116.00"), t.total());
    }

    @Test
    void ivaNoIncluidoSeSuma() {
        Totales t = Totales.de(new BigDecimal("116"), 16.0, false);
        assertEquals(new BigDecimal("134.56"), t.total());
    }

    @Test
    void subtotalMasIvaSiempreCuadra() {
        for (String precio : new String[]{"0.01", "9.99", "10.00", "17.50", "1234.56"}) {
            Totales t = Totales.de(new BigDecimal(precio), 16.0, true);
            assertEquals(0, t.subtotal().add(t.impuestos()).compareTo(t.total()), precio);
        }
    }

    @Test
    void tarjetasDePruebaAprobadas() {
        assertTrue(pasarela.cobrar(tarjeta("4242 4242 4242 4242", "12/30", "123"), BigDecimal.TEN, "t").aprobado());
        assertEquals("Mastercard", pasarela.cobrar(tarjeta("5555555555554444", "12/30", "123"), BigDecimal.TEN, "t").marca());
        assertTrue(pasarela.cobrar(tarjeta("378282246310005", "12/30", "1234"), BigDecimal.TEN, "t").aprobado());
    }

    @Test
    void tarjetasDePruebaRechazadas() {
        assertFalse(pasarela.cobrar(tarjeta("4000000000000002", "12/30", "123"), BigDecimal.TEN, "t").aprobado());
        assertFalse(pasarela.cobrar(tarjeta("4000000000009995", "12/30", "123"), BigDecimal.TEN, "t").aprobado());
    }

    @Test
    void datosInvalidos() {
        assertThrows(PagoRechazadoException.class, () -> pasarela.validar(tarjeta("4242424242424241", "12/30", "123"))); // Luhn
        assertThrows(PagoRechazadoException.class, () -> pasarela.validar(tarjeta("4242424242424242", "12/20", "123"))); // vencida
        assertThrows(PagoRechazadoException.class, () -> pasarela.validar(tarjeta("4242424242424242", "13/30", "123"))); // mes
        assertThrows(PagoRechazadoException.class, () -> pasarela.validar(tarjeta("4242424242424242", "12/30", "12")));  // cvv
        assertThrows(PagoRechazadoException.class, () -> pasarela.validar(null));
    }
}
