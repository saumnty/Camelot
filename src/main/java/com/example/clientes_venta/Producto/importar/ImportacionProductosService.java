package com.example.clientes_venta.Producto.importar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Producto.ProductoRepository;
import com.example.clientes_venta.Producto.ProductoService;
import com.example.clientes_venta.Tienda.Tienda;

/**
 * "Importar productos": archivo → vista previa → confirmar.
 * Nada se guarda hasta que el admin confirma.
 */
@Service
public class ImportacionProductosService {

    /** Máximo de códigos que se buscan en internet por archivo (Open Food Facts pide no saturarlo). */
    public static final int MAX_BUSQUEDAS = 80;

    public record VistaPrevia(List<FilaImportada> filas, List<String> errores,
                              long nuevos, long actualizar, long sinCambios, long conError,
                              long completados, boolean faltaronBusquedas) {
        public boolean hayAlgoQueGuardar() { return nuevos + actualizar > 0; }
        // getters estilo JavaBean para Thymeleaf
        public List<FilaImportada> getFilas() { return filas; }
        public List<String> getErrores() { return errores; }
        public long getNuevos() { return nuevos; }
        public long getActualizar() { return actualizar; }
        public long getSinCambios() { return sinCambios; }
        public long getConError() { return conError; }
        public long getCompletados() { return completados; }
        public boolean isFaltaronBusquedas() { return faltaronBusquedas; }
    }

    public record Resultado(int creados, int actualizados) {}

    private final ProductoRepository productoRepo;
    private final ProductoService productoService;
    private final CodigoBarrasService codigos;

    public ImportacionProductosService(ProductoRepository productoRepo,
                                       ProductoService productoService,
                                       CodigoBarrasService codigos) {
        this.productoRepo = productoRepo;
        this.productoService = productoService;
        this.codigos = codigos;
    }

    // ================================ 1) Leer ================================

    public List<List<String>> leerArchivo(MultipartFile archivo) throws IOException {
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            return LectorExcel.leer(archivo.getInputStream());
        }
        if (nombre.endsWith(".csv") || nombre.endsWith(".txt") || nombre.endsWith(".tsv")) {
            return TablaArchivo.leerCsv(archivo.getBytes());
        }
        throw new IllegalArgumentException("Sube un archivo de Excel (.xlsx) o CSV (.csv).");
    }

    // ============================ 2) Vista previa ============================

    @Transactional(readOnly = true)
    public VistaPrevia previsualizar(Tienda tienda, List<List<String>> tabla,
                                     boolean completarConCodigo, boolean actualizarExistentes) {
        MapeoColumnas.Resultado r = MapeoColumnas.convertir(tabla);
        List<FilaImportada> filas = r.filas();

        // a) Completar nombre / foto / medida con el código de barras
        boolean faltaron = false;
        long completados = 0;
        if (completarConCodigo) {
            List<FilaImportada> porBuscar = filas.stream()
                    .filter(f -> !f.tieneErrores() && Medidas.codigoValido(f.getCodigo())
                            && (f.getNombre() == null || f.getImagenUrl() == null))
                    .toList();
            faltaron = porBuscar.size() > MAX_BUSQUEDAS;
            completados = completar(porBuscar.subList(0, Math.min(porBuscar.size(), MAX_BUSQUEDAS)));
        }

        // b) ¿Nuevo o actualización? (por código de barras y, si no trae, por nombre)
        Map<String, Producto> porCodigo = new HashMap<>();
        Map<String, Producto> porNombre = new HashMap<>();
        for (Producto p : productoRepo.findByTienda_Id(tienda.getId())) {
            if (p.getCodigoBarras() != null) porCodigo.putIfAbsent(p.getCodigoBarras(), p);
            if (p.getName() != null) porNombre.putIfAbsent(MapeoColumnas.clave(p.getName()), p);
        }

        for (FilaImportada f : filas) {
            if (f.tieneErrores()) continue;
            Producto existente = null;
            if (f.getCodigo() != null) existente = porCodigo.get(f.getCodigo());
            if (existente == null && f.getNombre() != null) existente = porNombre.get(MapeoColumnas.clave(f.getNombre()));

            if (existente != null) {
                f.setProductoId(existente.getId());
                if (!actualizarExistentes) {
                    f.setAccion(FilaImportada.Accion.SIN_CAMBIOS);
                    f.aviso("Ya existe y elegiste no actualizar.");
                } else if (!hayCambios(f, existente)) {
                    f.setAccion(FilaImportada.Accion.SIN_CAMBIOS);
                } else {
                    f.setAccion(FilaImportada.Accion.ACTUALIZAR);
                }
            } else {
                if (f.getNombre() == null) {
                    f.error(f.getCodigo() != null
                            ? "Falta el nombre y no se encontró con el código de barras."
                            : "Falta el nombre.");
                } else if (f.getPrecio() == null) {
                    f.error("Falta el precio (es obligatorio para productos nuevos).");
                } else {
                    f.setAccion(FilaImportada.Accion.NUEVO);
                }
            }
        }

        return new VistaPrevia(filas, r.errores(),
                contar(filas, FilaImportada.Accion.NUEVO),
                contar(filas, FilaImportada.Accion.ACTUALIZAR),
                contar(filas, FilaImportada.Accion.SIN_CAMBIOS),
                contar(filas, FilaImportada.Accion.ERROR),
                completados, faltaron);
    }

    /** Busca hasta 4 códigos a la vez (hilos virtuales de Java 21). */
    private long completar(List<FilaImportada> filas) {
        if (filas.isEmpty()) return 0;
        Semaphore cupo = new Semaphore(4);
        List<Future<Optional<ProductoExterno>>> futuros = new ArrayList<>();
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (FilaImportada f : filas) {
                futuros.add(ex.submit(() -> {
                    cupo.acquire();
                    try {
                        return codigos.buscar(f.getCodigo());
                    } finally {
                        cupo.release();
                    }
                }));
            }
            long n = 0;
            for (int i = 0; i < filas.size(); i++) {
                Optional<ProductoExterno> encontrado;
                try {
                    encontrado = futuros.get(i).get();
                } catch (Exception e) {
                    encontrado = Optional.empty();
                }
                if (encontrado.isEmpty()) continue;
                FilaImportada f = filas.get(i);
                ProductoExterno p = encontrado.get();
                boolean cambio = false;
                if (f.getNombre() == null && p.nombre() != null) { f.setNombre(p.nombre()); cambio = true; }
                if (f.getImagenUrl() == null && p.imagenUrl() != null) { f.setImagenUrl(p.imagenUrl()); cambio = true; }
                if (f.getUnidad() == null && p.unidad() != null) f.setUnidad(p.unidad());
                if (f.getCantidad() == null && p.cantidad() != null) f.setCantidad(p.cantidad());
                if (cambio) { f.setCompletadoConCodigo(true); n++; }
            }
            return n;
        }
    }

    private static boolean hayCambios(FilaImportada f, Producto p) {
        return (f.getPrecio() != null && (p.getPrecio() == null || f.getPrecio().compareTo(p.getPrecio()) != 0))
                || (f.getCosto() != null && (p.getCostoProveedor() == null || f.getCosto().compareTo(p.getCostoProveedor()) != 0))
                || (f.getStock() != null && !f.getStock().equals(p.getStock()))
                || (f.getCodigo() != null && !f.getCodigo().equals(p.getCodigoBarras()))
                || (f.getImagenUrl() != null && !f.getImagenUrl().equals(p.getImagenUrl()))
                || (f.getUnidad() != null && !f.getUnidad().equals(p.getUnidadMedida()))
                || (f.getCantidad() != null && !Objects.equals(f.getCantidad(), p.getCantidadMedida()))
                || (f.getCategoria() != null && (p.getCategoria() == null
                        || !MapeoColumnas.clave(f.getCategoria()).equals(MapeoColumnas.clave(p.getCategoria().getNombre()))))
                || (f.getSubcategoria() != null && (p.getSubcategoria() == null
                        || !MapeoColumnas.clave(f.getSubcategoria()).equals(MapeoColumnas.clave(p.getSubcategoria().getNombre()))));
    }

    private static long contar(List<FilaImportada> filas, FilaImportada.Accion a) {
        return filas.stream().filter(f -> f.getAccion() == a).count();
    }

    // ============================== 3) Guardar ==============================

    /**
     * Guarda las filas NUEVO y ACTUALIZAR. En una sola transacción: si algo falla, no se guarda nada.
     * En actualizaciones solo se cambian los campos que traía el archivo (los vacíos se respetan).
     */
    @Transactional
    public Resultado aplicar(Tienda tienda, List<FilaImportada> filas) {
        int creados = 0, actualizados = 0;
        for (FilaImportada f : filas) {
            if (f.getAccion() == FilaImportada.Accion.NUEVO) {
                Producto p = new Producto();
                p.setTienda(tienda);
                p.setName(f.getNombre());
                copiar(f, p);
                productoService.guardarProducto(p, f.getCategoria(), f.getSubcategoria());
                creados++;
            } else if (f.getAccion() == FilaImportada.Accion.ACTUALIZAR && f.getProductoId() != null) {
                Producto p = productoRepo.findById(f.getProductoId()).orElse(null);
                // Seguridad: solo productos de ESTA tienda
                if (p == null || p.getTienda() == null || !p.getTienda().getId().equals(tienda.getId())) continue;
                copiar(f, p);
                // Si el archivo trae categoría/subcategoría, se usan esas; si no, se conservan las actuales.
                if (f.getCategoria() != null) {
                    p.setCategoria(null);
                    p.setSubcategoria(null); // la subcategoría vieja era de otra categoría
                }
                if (f.getSubcategoria() != null) p.setSubcategoria(null);
                productoService.guardarProducto(p, f.getCategoria(), f.getSubcategoria());
                actualizados++;
            }
        }
        return new Resultado(creados, actualizados);
    }

    private static void copiar(FilaImportada f, Producto p) {
        if (f.getPrecio() != null) p.setPrecio(f.getPrecio());
        if (f.getCosto() != null) p.setCostoProveedor(f.getCosto());
        if (f.getStock() != null) p.setStock(f.getStock());
        if (f.getCodigo() != null) p.setCodigoBarras(f.getCodigo());
        if (f.getImagenUrl() != null) p.setImagenUrl(f.getImagenUrl());
        if (f.getUnidad() != null) p.setUnidadMedida(f.getUnidad());
        if (f.getCantidad() != null) p.setCantidadMedida(f.getCantidad());
    }

    // ============================== Plantilla ==============================

    public static String plantillaCsv() {
        return "﻿" // BOM: así Excel abre bien los acentos
                + "codigo_barras,nombre,precio,costo,stock,categoria,subcategoria,unidad,cantidad,imagen_url\n"
                + "7501055300075,Coca-Cola 600 ml,20.00,15.50,48,Bebidas,Refrescos,ml,600,\n"
                + "7501000111206,,19.00,14.80,30,Panadería,Pastelitos,,,\n"
                + ",Tortillas de maíz (kg),24.00,19.00,,Tortillería,Tortillas,kg,1,\n";
    }
}
