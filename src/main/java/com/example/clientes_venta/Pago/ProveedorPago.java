package com.example.clientes_venta.Pago;

import java.math.BigDecimal;

/**
 * Contrato con una pasarela de pago. Hoy solo existe ProveedorPagoSimulado.
 *
 * Para cobrar con dinero real (Mercado Pago, Stripe, Openpay, Conekta...):
 *   1. Crea otra clase que implemente esta interfaz, con @Component y
 *      @ConditionalOnProperty(name = "camelot.pagos.proveedor", havingValue = "mercadopago").
 *   2. En la página de pago usa el formulario/SDK de JavaScript del proveedor: él recibe
 *      la tarjeta y te devuelve un TOKEN. Tu servidor nunca ve el número de tarjeta
 *      (así no necesitas certificarte en PCI-DSS).
 *   3. Aquí recibes ese token en DatosTarjeta.token y llamas a la API del proveedor.
 *   4. Cambia camelot.pagos.proveedor en application.properties (o variable de entorno en Render).
 */
public interface ProveedorPago {

    /** Nombre que se guarda en Pago.proveedor. */
    String nombre();

    /** Revisa los datos antes de crear pedidos (formato, vigencia). Lanza PagoRechazadoException si no sirven. */
    void validar(DatosTarjeta tarjeta);

    ResultadoCobro cobrar(DatosTarjeta tarjeta, BigDecimal monto, String descripcion);

    ResultadoCobro reembolsar(String referencia, BigDecimal monto);

    /**
     * Datos de la tarjeta tal como llegan del formulario.
     * Con un proveedor real solo vendría "token" (el número nunca toca tu servidor).
     */
    record DatosTarjeta(String numero, String titular, String vencimiento, String cvv, String token) {}

    record ResultadoCobro(boolean aprobado, String referencia, String marca, String ultimos4, String mensaje) {}
}
