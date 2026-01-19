package com.example.clientes_venta.Subcategoria;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubcategoriaController {

    private final SubcategoriaService subcategoriaService;

    public SubcategoriaController(SubcategoriaService subcategoriaService) {
        this.subcategoriaService = subcategoriaService;
    }

    @GetMapping("/api/subcategorias")
    public List<String> subcategoriasPorCategoria(@RequestParam String categoriaNombre) {
        return subcategoriaService.listarNombresPorCategoriaNombre(categoriaNombre);
    }
}