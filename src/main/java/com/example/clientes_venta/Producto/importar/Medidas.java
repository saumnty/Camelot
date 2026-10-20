package com.example.clientes_venta.Producto.importar;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidades sin dependencias (se pueden probar sin Spring):
 * unidades de medida, números escritos "a la mexicana" y textos sin acentos.
 */
public final class Medidas {

    private Medidas() {}

    /** Cantidad + unidad, p. ej. "600 ml" → (600, "ml"). */
    public record Medida(Double cantidad, String unidad) {}

    private static final Pattern CANTIDAD = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(ml|mililitros?|l|lt|lts|litros?|kg|kilos?|kilogramos?|g|gr|grs|gramos?|pzas?|piezas?|pz|u|unidades?)\\b",
            Pattern.CASE_INSENSITIVE);

    /** Lee textos como "600 ml", "1.5L", "Paquete 12 pzas", "500g". Devuelve null si no entiende. */
    public static Medida leer(String texto) {
        if (texto == null || texto.isBlank()) return null;
        Matcher m = CANTIDAD.matcher(sinAcentos(texto));
        if (!m.find()) return null;
        Double cantidad = Double.valueOf(m.group(1).replace(',', '.'));
        return new Medida(cantidad, normalizarUnidad(m.group(2)));
    }

    /**
     * Las unidades que acepta el formulario de producto: pieza, l, ml, kg, g.
     * Devuelve null si la unidad no se reconoce.
     */
    public static String normalizarUnidad(String u) {
        if (u == null || u.isBlank()) return null;
        String x = sinAcentos(u).trim().toLowerCase(Locale.ROOT).replace(".", "");
        return switch (x) {
            case "ml", "mililitro", "mililitros" -> "ml";
            case "l", "lt", "lts", "litro", "litros" -> "l";
            case "kg", "kilo", "kilos", "kilogramo", "kilogramos" -> "kg";
            case "g", "gr", "grs", "gramo", "gramos" -> "g";
            case "pza", "pzas", "pz", "pieza", "piezas", "u", "unidad", "unidades", "pzs" -> "pieza";
            default -> null;
        };
    }

    /**
     * Convierte "$1,234.50", "1234,5", "12,50", " 20 " en número. Null si viene vacío.
     * Lanza NumberFormatException si no es un número.
     */
    public static java.math.BigDecimal dinero(String s) {
        if (s == null) return null;
        String t = s.trim().replace("$", "").replace("MXN", "").replace("mxn", "").replace(" ", "").replace(" ", "");
        if (t.isEmpty()) return null;
        boolean coma = t.contains(","), punto = t.contains(".");
        if (coma && punto) {
            // el que va al final es el separador decimal
            if (t.lastIndexOf(',') > t.lastIndexOf('.')) t = t.replace(".", "").replace(',', '.');
            else t = t.replace(",", "");
        } else if (coma) {
            int decimales = t.length() - t.lastIndexOf(',') - 1;
            t = (decimales == 1 || decimales == 2) ? t.replace(',', '.') : t.replace(",", "");
        }
        java.math.BigDecimal v = new java.math.BigDecimal(t);
        return v.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** "12", "12.0" (Excel), "1,200" → 12 / 1200. Null si viene vacío. */
    public static Integer entero(String s) {
        java.math.BigDecimal v = dinero(s);
        if (v == null) return null;
        return v.setScale(0, java.math.RoundingMode.DOWN).intValueExact();
    }

    public static Double decimal(String s) {
        java.math.BigDecimal v = dinero(s);
        return v == null ? null : v.stripTrailingZeros().doubleValue();
    }

    /** "Código de Barras" → "codigo de barras" */
    public static String sinAcentos(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Solo dígitos; null si queda vacío. Acepta "7501055300075.0" que a veces exporta Excel. */
    public static String codigoBarras(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.matches("\\d+\\.0+")) t = t.substring(0, t.indexOf('.'));
        if (t.matches("(?i)\\d(\\.\\d+)?e\\+?\\d+")) {          // 7.501055E+12 (Excel en notación científica)
            t = new java.math.BigDecimal(t).toPlainString();
        }
        t = t.replaceAll("\\D", "");
        return t.isEmpty() ? null : t;
    }

    /** EAN-8, UPC-A (12), EAN-13 o GTIN-14 */
    public static boolean codigoValido(String codigo) {
        return codigo != null && codigo.matches("\\d{8}|\\d{12,14}");
    }
}
