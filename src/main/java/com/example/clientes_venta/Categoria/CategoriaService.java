package com.example.clientes_venta.Categoria;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    @Transactional
    public Categoria obtenerOCrear(String nombre) {
        if (nombre == null) return null;
        String n = nombre.trim();
        if (n.isEmpty()) return null;

        return categoriaRepository
                .findByNombreIgnoreCase(n)
                .orElseGet(() -> categoriaRepository.save(new Categoria(n)));
    }
}