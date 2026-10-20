package com.example.clientes_venta.Producto.importar;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Producto.ProductoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Busca un producto por código de barras:
 *   1) en Camelot (si otra tienda ya lo dio de alta, reusa su nombre y foto),
 *   2) en Open Food Facts (base abierta y gratuita, con muchos productos mexicanos).
 * Guarda los resultados en memoria para no repetir consultas.
 */
@Service
public class CodigoBarrasService {

    private static final Logger log = LoggerFactory.getLogger(CodigoBarrasService.class);
    private static final String API = "https://world.openfoodfacts.org/api/v2/product/";
    private static final String CAMPOS = "product_name_es,product_name,brands,quantity,image_front_url,image_url";
    private static final int MAX_CACHE = 5000;

    private final ProductoRepository productoRepo;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Map<String, Optional<ProductoExterno>> cache = new ConcurrentHashMap<>();

    @Value("${camelot.openfoodfacts.habilitado:true}")
    private boolean offHabilitado;

    public CodigoBarrasService(ProductoRepository productoRepo, ObjectMapper json) {
        this.productoRepo = productoRepo;
        this.json = json;
    }

    public Optional<ProductoExterno> buscar(String codigoCrudo) {
        String codigo = Medidas.codigoBarras(codigoCrudo);
        if (!Medidas.codigoValido(codigo)) return Optional.empty();

        // 1) Camelot
        Optional<Producto> propio = productoRepo.findFirstByCodigoBarrasOrderByIdDesc(codigo);
        if (propio.isPresent() && propio.get().getName() != null) {
            Producto p = propio.get();
            return Optional.of(new ProductoExterno(codigo, p.getName(), null, p.getCantidadMedida(),
                    p.getUnidadMedida(), p.getImagenUrl(), "camelot"));
        }

        // 2) Open Food Facts (con caché)
        if (!offHabilitado) return Optional.empty();
        Optional<ProductoExterno> enCache = cache.get(codigo);
        if (enCache != null) return enCache;

        Optional<ProductoExterno> r = consultarOff(codigo);
        if (cache.size() > MAX_CACHE) cache.clear();
        cache.put(codigo, r);
        return r;
    }

    private Optional<ProductoExterno> consultarOff(String codigo) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(API + codigo + "?fields=" + CAMPOS))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "Camelot/1.0 (tiendas de abarrotes; proyecto escolar UAM)")
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return Optional.empty();

            JsonNode raiz = json.readTree(res.body());
            if (raiz.path("status").asInt(0) != 1) return Optional.empty();
            JsonNode p = raiz.path("product");

            String nombre = texto(p, "product_name_es");
            if (nombre == null) nombre = texto(p, "product_name");
            String marca = texto(p, "brands");
            if (marca != null && marca.contains(",")) marca = marca.substring(0, marca.indexOf(',')).trim();
            String cantidadTxt = texto(p, "quantity");
            String imagen = texto(p, "image_front_url");
            if (imagen == null) imagen = texto(p, "image_url");
            if (nombre == null && imagen == null) return Optional.empty();

            // "Coca-Cola" + "Coca-Cola" + "600 ml" → "Coca-Cola 600 ml"
            StringBuilder completo = new StringBuilder(nombre == null ? "" : nombre);
            if (marca != null && !completo.toString().toLowerCase(Locale.ROOT).contains(marca.toLowerCase(Locale.ROOT))) {
                completo.append(completo.length() == 0 ? "" : " ").append(marca);
            }
            Medidas.Medida m = Medidas.leer(cantidadTxt);
            if (cantidadTxt != null && Medidas.leer(completo.toString()) == null) {
                completo.append(" ").append(cantidadTxt);
            }
            String nombreFinal = completo.toString().trim();
            return Optional.of(new ProductoExterno(codigo, nombreFinal.isEmpty() ? null : nombreFinal, marca,
                    m == null ? null : m.cantidad(), m == null ? null : m.unidad(), imagen, "openfoodfacts"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            // Sin internet, OFF caído, JSON raro... no debe tumbar nada: simplemente no se autollena
            log.info("[codigo-barras] No se pudo consultar {}: {}", codigo, e.toString());
            return Optional.empty();
        }
    }

    private static String texto(JsonNode n, String campo) {
        String v = n.path(campo).asText(null);
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
