package com.example.clientes_venta.Producto.importar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Convierte la tabla (primera fila = encabezados) en FilaImportada.
 * Los encabezados se reconocen aunque vengan con acentos, mayúsculas o con otro nombre común
 * ("Descripción", "Precio venta", "Existencia", "EAN"...), así sirve el Excel que ya tenga la tienda.
 */
public final class MapeoColumnas {

    private MapeoColumnas() {}

    public static final int MAX_FILAS = 2000;

    public record Resultado(List<FilaImportada> filas, List<String> errores, Set<String> columnasReconocidas) {}

    private static final Map<String, String> SINONIMOS = new HashMap<>();
    static {
        sin("codigo", "codigo", "codigo de barras", "codigo_barras", "codigobarras", "cod barras", "ean", "ean13", "upc", "barcode", "gtin", "sku");
        sin("nombre", "nombre", "producto", "nombre del producto", "descripcion", "articulo", "name");
        sin("precio", "precio", "precio venta", "precio de venta", "precio_venta", "pventa", "p venta", "precio publico", "price");
        sin("costo", "costo", "costo proveedor", "costo_proveedor", "precio compra", "precio de compra", "costo unitario", "cost");
        sin("stock", "stock", "existencia", "existencias", "inventario", "cantidad en stock", "piezas", "qty");
        sin("categoria", "categoria", "departamento", "familia", "category");
        sin("subcategoria", "subcategoria", "sub categoria", "linea", "marca");
        sin("unidad", "unidad", "unidad de medida", "unidad_medida", "um");
        sin("cantidad", "cantidad", "contenido", "contenido neto", "tamano", "presentacion", "cantidad_medida");
        sin("imagen", "imagen", "imagen url", "imagen_url", "foto", "url imagen", "image");
    }

    private static void sin(String campo, String... nombres) {
        for (String n : nombres) SINONIMOS.put(clave(n), campo);
    }

    static String clave(String s) {
        return Medidas.sinAcentos(s).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static Resultado convertir(List<List<String>> tabla) {
        List<String> errores = new ArrayList<>();
        if (tabla == null || tabla.isEmpty()) {
            errores.add("El archivo está vacío.");
            return new Resultado(List.of(), errores, Set.of());
        }

        // 1) Encabezados: la primera fila que no esté vacía
        int filaEnc = 0;
        while (filaEnc < tabla.size() && tabla.get(filaEnc).isEmpty()) filaEnc++;
        if (filaEnc == tabla.size()) {
            errores.add("El archivo está vacío.");
            return new Resultado(List.of(), errores, Set.of());
        }
        Map<String, Integer> col = new HashMap<>();
        List<String> enc = tabla.get(filaEnc);
        for (int i = 0; i < enc.size(); i++) {
            String campo = SINONIMOS.get(clave(enc.get(i)));
            if (campo != null && !col.containsKey(campo)) col.put(campo, i);
        }
        if (!col.containsKey("nombre") && !col.containsKey("codigo")) {
            errores.add("No encontré la columna \"nombre\" ni \"codigo\". La primera fila debe tener los encabezados "
                    + "(descarga la plantilla para ver un ejemplo).");
            return new Resultado(List.of(), errores, col.keySet());
        }
        long datos = tabla.stream().skip(filaEnc + 1L).filter(f -> !f.isEmpty()).count();
        if (datos > MAX_FILAS) {
            errores.add("El archivo tiene " + datos + " productos; el máximo por archivo es " + MAX_FILAS
                    + ". Divídelo en varios archivos.");
            return new Resultado(List.of(), errores, col.keySet());
        }

        // 2) Filas
        List<FilaImportada> filas = new ArrayList<>();
        for (int r = filaEnc + 1; r < tabla.size(); r++) {
            List<String> row = tabla.get(r);
            if (row.isEmpty()) continue;
            FilaImportada f = new FilaImportada();
            f.setNumero(r + 1);   // como lo numera Excel

            f.setCodigo(Medidas.codigoBarras(valor(row, col, "codigo")));
            f.setNombre(recortar(valor(row, col, "nombre"), 200));
            f.setCategoria(recortar(valor(row, col, "categoria"), 80));
            f.setSubcategoria(recortar(valor(row, col, "subcategoria"), 80));
            f.setImagenUrl(recortar(valor(row, col, "imagen"), 1000));

            numero(f, "precio", () -> f.setPrecio(Medidas.dinero(valor(row, col, "precio"))));
            numero(f, "costo", () -> f.setCosto(Medidas.dinero(valor(row, col, "costo"))));
            numero(f, "stock", () -> f.setStock(Medidas.entero(valor(row, col, "stock"))));

            // Unidad y cantidad: columnas separadas o "600 ml" en una sola
            String unidadTxt = valor(row, col, "unidad");
            String cantidadTxt = valor(row, col, "cantidad");
            String unidad = Medidas.normalizarUnidad(unidadTxt);
            Double cantidad = null;
            if (cantidadTxt != null && !cantidadTxt.isBlank()) {
                try {
                    cantidad = Medidas.decimal(cantidadTxt);
                } catch (NumberFormatException e) {
                    Medidas.Medida m = Medidas.leer(cantidadTxt);
                    if (m != null) { cantidad = m.cantidad(); if (unidad == null) unidad = m.unidad(); }
                    else f.aviso("No entendí el contenido \"" + cantidadTxt + "\"; se deja vacío.");
                }
            }
            if (unidadTxt != null && !unidadTxt.isBlank() && unidad == null) {
                f.aviso("Unidad \"" + unidadTxt + "\" no reconocida (usa pieza, l, ml, kg o g); se deja vacía.");
            }
            if ((unidad == null || cantidad == null) && f.getNombre() != null) {
                Medidas.Medida m = Medidas.leer(f.getNombre());       // "Coca-Cola 600 ml"
                if (m != null) {
                    if (unidad == null) unidad = m.unidad();
                    if (cantidad == null) cantidad = m.cantidad();
                }
            }
            f.setUnidad(unidad);
            f.setCantidad(cantidad);

            if (f.getPrecio() != null && f.getPrecio().signum() < 0) f.error("El precio no puede ser negativo.");
            if (f.getCosto() != null && f.getCosto().signum() < 0) f.error("El costo no puede ser negativo.");
            if (f.getStock() != null && f.getStock() < 0) f.error("El stock no puede ser negativo.");
            if (f.getCodigo() != null && !Medidas.codigoValido(f.getCodigo())) {
                f.aviso("El código \"" + f.getCodigo() + "\" no parece EAN/UPC; se guarda tal cual.");
            }
            if (f.getImagenUrl() != null && !f.getImagenUrl().startsWith("https://") && !f.getImagenUrl().startsWith("http://")) {
                f.aviso("La imagen debe ser un enlace http(s); se ignora.");
                f.setImagenUrl(null);
            }
            filas.add(f);
        }

        // 3) Repetidos dentro del mismo archivo
        Map<String, Integer> vistos = new HashMap<>();
        for (FilaImportada f : filas) {
            String k = f.getCodigo() != null ? "c:" + f.getCodigo()
                    : f.getNombre() != null ? "n:" + clave(f.getNombre()) : null;
            if (k == null) continue;
            Integer antes = vistos.putIfAbsent(k, f.getNumero());
            if (antes != null) f.error("Repetido: ya viene en la fila " + antes + ".");
        }
        return new Resultado(filas, errores, new HashSet<>(col.keySet()));
    }

    private interface Accion { void run(); }

    private static void numero(FilaImportada f, String campo, Accion a) {
        try {
            a.run();
        } catch (NumberFormatException | ArithmeticException e) {
            f.error("\"" + campo + "\" no es un número válido.");
        }
    }

    private static String valor(List<String> row, Map<String, Integer> col, String campo) {
        Integer i = col.get(campo);
        if (i == null || i >= row.size()) return null;
        String v = row.get(i);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
