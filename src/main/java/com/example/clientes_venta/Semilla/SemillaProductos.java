package com.example.clientes_venta.Semilla;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Producto.ProductoRepository;
import com.example.clientes_venta.Producto.ProductoService;
import com.example.clientes_venta.Producto.importar.Medidas;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaRepository;

/**
 * Llena el catálogo de UNA tienda con productos de un CSV. Solo corre si le dices a qué tienda:
 *
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--camelot.semilla.tienda-id=5"
 *
 * (o en Render, agrega la variable CAMELOT_SEMILLA_TIENDA_ID=5, despliega una vez y luego bórrala).
 *
 * Por defecto usa src/main/resources/semilla/abarrotes.csv. Para usar el que genera
 * scripts/openfoodfacts_a_csv.py:  --camelot.semilla.archivo=file:/ruta/productos_off.csv
 *
 * No duplica: si la tienda ya tiene un producto con el mismo nombre, lo salta.
 */
@Component
@ConditionalOnProperty(name = "camelot.semilla.tienda-id")
public class SemillaProductos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SemillaProductos.class);

    private final TiendaRepository tiendaRepo;
    private final ProductoRepository productoRepo;
    private final ProductoService productoService;
    private final ResourceLoader resourceLoader;

    @Value("${camelot.semilla.tienda-id}")
    private Long tiendaId;

    @Value("${camelot.semilla.archivo:classpath:semilla/abarrotes.csv}")
    private String archivo;

    public SemillaProductos(TiendaRepository tiendaRepo, ProductoRepository productoRepo,
                            ProductoService productoService, ResourceLoader resourceLoader) {
        this.tiendaRepo = tiendaRepo;
        this.productoRepo = productoRepo;
        this.productoService = productoService;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Tienda tienda = tiendaRepo.findById(tiendaId).orElse(null);
        if (tienda == null) {
            log.warn("[semilla] La tienda {} no existe; no se cargó nada.", tiendaId);
            return;
        }

        Set<String> existentes = new HashSet<>();
        productoRepo.findByTienda_Id(tiendaId).forEach(p -> existentes.add(p.getName().trim().toLowerCase()));

        Resource res = resourceLoader.getResource(archivo);
        int creados = 0, saltados = 0, invalidos = 0;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = in.readLine()) != null) {
                if (linea.isBlank() || linea.startsWith("#")) continue;
                String[] c = linea.split(";", -1);
                if (c.length < 9) { invalidos++; continue; }

                String nombre = c[0].trim();
                if (nombre.isEmpty() || existentes.contains(nombre.toLowerCase())) { saltados++; continue; }

                try {
                    Producto p = new Producto();
                    p.setName(nombre);
                    p.setPrecio(decimal(c[3]));
                    p.setCostoProveedor(decimal(c[4]));
                    p.setStock(c[5].isBlank() ? null : Integer.valueOf(c[5].trim()));
                    p.setUnidadMedida(Medidas.normalizarUnidad(c[6]));   // "L" → "l", "pza" → "pieza"
                    p.setCantidadMedida(c[7].isBlank() ? null : Double.valueOf(c[7].trim()));
                    p.setImagenUrl(c[8].isBlank() ? null : c[8].trim());
                    p.setTienda(tienda);
                    productoService.guardarProducto(p, c[1].trim(), c[2].trim());
                    existentes.add(nombre.toLowerCase());
                    creados++;
                } catch (RuntimeException e) {
                    invalidos++;
                    log.warn("[semilla] Línea ignorada ({}): {}", e.getMessage(), linea);
                }
            }
        }
        log.info("[semilla] Tienda '{}': {} productos nuevos, {} ya existían, {} líneas inválidas.",
                tienda.getNombre(), creados, saltados, invalidos);
    }

    private static BigDecimal decimal(String s) {
        return (s == null || s.isBlank()) ? null : new BigDecimal(s.trim());
    }
}
