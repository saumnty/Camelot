package com.example.clientes_venta.Producto.importar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Lectura de archivos sin base de datos: números "a la mexicana", encabezados con otros nombres, etc. */
class ImportarArchivoTest {

    private static List<FilaImportada> leer(String csv, Charset cs) {
        return MapeoColumnas.convertir(TablaArchivo.leerCsv(csv.getBytes(cs))).filas();
    }

    @Test
    void numerosEnVariosFormatos() {
        assertEquals(new BigDecimal("1234.50"), Medidas.dinero("$1,234.50"));
        assertEquals(new BigDecimal("12.50"), Medidas.dinero("12,50"));
        assertEquals(new BigDecimal("1234.50"), Medidas.dinero("1.234,5"));
        assertEquals(new BigDecimal("1200.00"), Medidas.dinero("1,200"));
        assertNull(Medidas.dinero("  "));
        assertEquals(12, Medidas.entero("12.0"));
    }

    @Test
    void codigosQueExcelEchaAPerder() {
        assertEquals("7501055300075", Medidas.codigoBarras("7501055300075.0"));
        assertEquals("7501055300075", Medidas.codigoBarras("7.501055300075E+12"));
        assertTrue(Medidas.codigoValido("7501055300075"));
        assertFalse(Medidas.codigoValido("123"));
    }

    @Test
    void medidasDentroDelTexto() {
        assertEquals(new Medidas.Medida(600.0, "ml"), Medidas.leer("Coca-Cola 600 ml"));
        assertEquals(new Medidas.Medida(1.5, "l"), Medidas.leer("Agua 1.5L"));
        assertEquals(new Medidas.Medida(18.0, "pieza"), Medidas.leer("Huevo 18 pzas"));
        assertNull(Medidas.leer("Sabritas"));
    }

    @Test
    void encabezadosConOtrosNombresYFilasDeExcel() {
        String csv = "﻿Código de barras,Descripción,Precio venta,Existencia,Departamento,Contenido\n"
                + "7501055300075,\"Coca-Cola, 600 ml\",$20.00,48,Bebidas,600 ml\n"
                + "# comentario\n"
                + "\n"
                + "7501000111206,Gansito,\"19,00\",30,Pan,\n"
                + ",Hielo 5 kg,35,,Varios,\n"
                + ",Malo,abc,-1,,\n"
                + "7501055300075,Repetida,1,1,,\n";
        List<FilaImportada> f = leer(csv, StandardCharsets.UTF_8);
        assertEquals(5, f.size());
        assertEquals("Coca-Cola, 600 ml", f.get(0).getNombre());
        assertEquals("ml", f.get(0).getUnidad());
        assertEquals(5, f.get(1).getNumero(), "número de fila como en Excel");
        assertEquals(new BigDecimal("19.00"), f.get(1).getPrecio());
        assertEquals("kg", f.get(2).getUnidad());
        assertTrue(f.get(3).tieneErrores());
        assertTrue(f.get(4).getErrores().get(0).contains("fila 2"));
    }

    @Test
    void puntoYComaYAcentosDeWindows() {
        List<FilaImportada> f = leer("nombre;precio;stock\nAzúcar estándar;33,00;20\n", Charset.forName("windows-1252"));
        assertEquals("Azúcar estándar", f.get(0).getNombre());
        assertEquals(new BigDecimal("33.00"), f.get(0).getPrecio());
    }

    @Test
    void sinColumnasConocidasDaError() {
        var r = MapeoColumnas.convertir(TablaArchivo.leerCsv("foo,bar\n1,2\n".getBytes(StandardCharsets.UTF_8)));
        assertFalse(r.errores().isEmpty());
    }
}
