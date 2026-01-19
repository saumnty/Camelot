package com.example.clientes_venta.Subcategoria;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Categoria.Categoria;

@Service
public class SubcategoriaService {

    private final SubcategoriaRepository subcategoriaRepository;

    public SubcategoriaService(SubcategoriaRepository subcategoriaRepository) {
        this.subcategoriaRepository = subcategoriaRepository;
    }

    public List<Subcategoria> listar() {
        return subcategoriaRepository.findAll();
    }

    public List<Subcategoria> listarPorCategoriaId(Long categoriaId) {
        if (categoriaId == null) return List.of();
        return subcategoriaRepository.findByCategoriaIdOrderByNombreAsc(categoriaId);
    }

    public List<String> listarNombresPorCategoriaNombre(String categoriaNombre) {
        if (categoriaNombre == null) return List.of();
        String n = categoriaNombre.trim();
        if (n.isEmpty()) return List.of();
        return subcategoriaRepository.findNombresByCategoriaNombreIgnoreCase(n);
    }

    @Transactional
    public Subcategoria obtenerOCrear(String nombreSubcategoria, Categoria categoria) {
        if (categoria == null) return null;
        if (nombreSubcategoria == null) return null;

        String n = nombreSubcategoria.trim();
        if (n.isEmpty()) return null;

        return subcategoriaRepository
                .findByCategoriaIdAndNombreIgnoreCase(categoria.getId(), n)
                .orElseGet(() -> subcategoriaRepository.save(new Subcategoria(n, categoria)));
    }
}