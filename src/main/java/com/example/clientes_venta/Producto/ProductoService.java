package com.example.clientes_venta.Producto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Categoria.Categoria;
import com.example.clientes_venta.Categoria.CategoriaRepository;
import com.example.clientes_venta.Categoria.CategoriaService;
import com.example.clientes_venta.Subcategoria.Subcategoria;
import com.example.clientes_venta.Subcategoria.SubcategoriaRepository;
import com.example.clientes_venta.Subcategoria.SubcategoriaService;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    // ✅ para resolver categoria/subcategoria al guardar
    private final CategoriaService categoriaService;
    private final SubcategoriaService subcategoriaService;
    private final CategoriaRepository categoriaRepo;
    private final SubcategoriaRepository subcategoriaRepo;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaService categoriaService,
            SubcategoriaService subcategoriaService,
            CategoriaRepository categoriaRepo,
            SubcategoriaRepository subcategoriaRepo
    ) {
        this.productoRepository = productoRepository;
        this.categoriaService = categoriaService;
        this.subcategoriaService = subcategoriaService;
        this.categoriaRepo = categoriaRepo;
        this.subcategoriaRepo = subcategoriaRepo;
    }

    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    public Optional<Producto> obtenerProductoPorId(Long id) {
        return productoRepository.findById(id);
    }

    public void eliminarProducto(Long id) {
        productoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, List<Producto>>> listarAgrupadoPorCategoriaYSubcategoria() {
        List<Producto> productos = productoRepository.findAllConCategoriaSubcategoriaOrdenado();
        return agruparPorCategoriaYSubcategoria(productos);
    }

    // ====== POR TIENDA ======
    public List<Producto> listarProductosPorTienda(Long tiendaId) {
        return productoRepository.findByTienda_Id(tiendaId);
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, List<Producto>>> listarAgrupadoPorTienda(Long tiendaId) {
        List<Producto> lista = productoRepository.findByTiendaOrdenado(tiendaId);
        return agruparPorCategoriaYSubcategoria(lista);
    }

    // ====== MARKETPLACE (SOLO NORMALES) ======
    public List<Producto> listarProductosMarketplace(String q) {
        return productoRepository.findAllMarketplace(q);
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, List<Producto>>> listarAgrupadoMarketplace(String q) {
        List<Producto> lista = productoRepository.findAllMarketplace(q);
        return agruparPorCategoriaYSubcategoria(lista);
    }

    // ====== TOP productos (para detalle tienda) ======
    public List<Producto> topProductosParaDetalle(Long tiendaId) {
        return productoRepository.findByTiendaOrdenado(tiendaId)
                .stream()
                .limit(6)
                .toList();
    }

    // ✅ ESTE ERA EL QUE TE FALTABA (tu controlador lo llama)
    @Transactional
    public Producto guardarProducto(Producto producto, String categoriaNombre, String subcategoriaNombre) {

        // 1) Resolver categoria (prioridad: ID seleccionada -> nombre escrito -> null)
        Categoria categoria = null;

        if (producto.getCategoria() != null && producto.getCategoria().getId() != null) {
            categoria = categoriaRepo.findById(producto.getCategoria().getId()).orElse(null);
        }
        if (categoria == null && categoriaNombre != null && !categoriaNombre.isBlank()) {
            categoria = categoriaService.obtenerOCrear(categoriaNombre);
        }
        producto.setCategoria(categoria);

        // 2) Resolver subcategoria (solo si hay categoria)
        Subcategoria sub = null;

        if (categoria != null) {
            if (producto.getSubcategoria() != null && producto.getSubcategoria().getId() != null) {
                sub = subcategoriaRepo.findById(producto.getSubcategoria().getId()).orElse(null);

                // si la subcategoria no pertenece a esa categoria, la ignoramos
                if (sub != null && sub.getCategoria() != null && !sub.getCategoria().getId().equals(categoria.getId())) {
                    sub = null;
                }
            }

            if (sub == null && subcategoriaNombre != null && !subcategoriaNombre.isBlank()) {
                sub = subcategoriaService.obtenerOCrear(subcategoriaNombre, categoria);
            }
        }

        producto.setSubcategoria(sub);

        // 3) Guardar
        return productoRepository.save(producto);
    }

    // ====== helper ======
    private Map<String, Map<String, List<Producto>>> agruparPorCategoriaYSubcategoria(List<Producto> productos) {

        Map<String, Map<String, List<Producto>>> catalogo = new LinkedHashMap<>();

        for (Producto p : productos) {
            String cat = (p.getCategoria() != null && p.getCategoria().getNombre() != null && !p.getCategoria().getNombre().isBlank())
                    ? p.getCategoria().getNombre()
                    : "Sin categoría";

            String sub = (p.getSubcategoria() != null && p.getSubcategoria().getNombre() != null && !p.getSubcategoria().getNombre().isBlank())
                    ? p.getSubcategoria().getNombre()
                    : "Sin subcategoría";

            catalogo
                .computeIfAbsent(cat, k -> new LinkedHashMap<>())
                .computeIfAbsent(sub, k -> new ArrayList<>())
                .add(p);
        }

        return catalogo;
    }
}
