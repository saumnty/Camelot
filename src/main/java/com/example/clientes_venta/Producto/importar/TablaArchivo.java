package com.example.clientes_venta.Producto.importar;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee un CSV a una tabla de textos. Sin dependencias para poder probarlo solo.
 * - Detecta el separador (coma, punto y coma o tabulador).
 * - Acepta comillas ("Galletas, surtidas") y saltos de línea dentro de comillas.
 * - Acepta UTF-8 (con o sin BOM) y el Windows-1252 que a veces guarda Excel en español.
 * - Las líneas vacías y las que empiezan con # se devuelven como filas vacías (se ignoran después).
 */
public final class TablaArchivo {

    private TablaArchivo() {}

    public static List<List<String>> leerCsv(byte[] bytes) {
        String texto = decodificar(bytes);
        char sep = detectarSeparador(texto);
        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean comillas = false;

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (comillas) {
                if (c == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') { celda.append('"'); i++; }
                    else comillas = false;
                } else {
                    celda.append(c);
                }
            } else if (c == '"') {
                comillas = true;
            } else if (c == sep) {
                fila.add(celda.toString()); celda.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') i++;
                fila.add(celda.toString()); celda.setLength(0);
                agregar(filas, fila);
                fila = new ArrayList<>();
            } else {
                celda.append(c);
            }
        }
        fila.add(celda.toString());
        agregar(filas, fila);
        return filas;
    }

    /** Las líneas vacías o de comentario se guardan como lista vacía para no perder el número de fila. */
    private static void agregar(List<List<String>> filas, List<String> fila) {
        boolean vacia = fila.stream().allMatch(String::isBlank);
        if (vacia || fila.get(0).trim().startsWith("#")) {
            filas.add(List.of());
            return;
        }
        filas.add(fila.stream().map(String::trim).toList());
    }

    static String decodificar(byte[] bytes) {
        int inicio = (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) ? 3 : 0;
        ByteBuffer buf = ByteBuffer.wrap(bytes, inicio, bytes.length - inicio);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(buf).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, inicio, bytes.length - inicio, Charset.forName("windows-1252"));
        }
    }

    /** El separador que más aparece en la primera línea (fuera de comillas). */
    static char detectarSeparador(String texto) {
        int fin = texto.indexOf('\n');
        String primera = fin < 0 ? texto : texto.substring(0, fin);
        // saltar líneas de comentario
        int desde = 0;
        while (primera.trim().startsWith("#") && fin >= 0) {
            desde = fin + 1;
            fin = texto.indexOf('\n', desde);
            primera = fin < 0 ? texto.substring(desde) : texto.substring(desde, fin);
        }
        int comas = 0, pyc = 0, tabs = 0;
        boolean q = false;
        for (char c : primera.toCharArray()) {
            if (c == '"') q = !q;
            if (q) continue;
            if (c == ',') comas++;
            else if (c == ';') pyc++;
            else if (c == '\t') tabs++;
        }
        if (tabs > comas && tabs > pyc) return '\t';
        return pyc > comas ? ';' : ',';
    }
}
