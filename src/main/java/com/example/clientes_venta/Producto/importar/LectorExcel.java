package com.example.clientes_venta.Producto.importar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/** Lee la PRIMERA hoja de un .xlsx/.xls como tabla de textos (igual que TablaArchivo.leerCsv). */
public final class LectorExcel {

    private LectorExcel() {}

    public static List<List<String>> leer(InputStream in) throws IOException {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet hoja = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter(java.util.Locale.US); // 1234.5 sin separador de miles
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();

            List<List<String>> tabla = new ArrayList<>();
            int ultima = hoja.getLastRowNum();
            for (int r = 0; r <= ultima; r++) {
                Row row = hoja.getRow(r);
                if (row == null) { tabla.add(List.of()); continue; }
                List<String> celdas = new ArrayList<>();
                int max = Math.max(row.getLastCellNum(), 0);
                for (int c = 0; c < max; c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String v = cell == null ? "" : fmt.formatCellValue(cell, eval).trim();
                    celdas.add(v);
                }
                boolean vacia = celdas.stream().allMatch(String::isBlank);
                tabla.add(vacia || celdas.get(0).startsWith("#") ? List.of() : celdas);
            }
            return tabla;
        }
    }
}
