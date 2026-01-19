package com.example.clientes_venta.Clientes;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ClientesController {

    private final ClienteService clienteService;

    public ClientesController(ClienteService clienteService){
        this.clienteService = clienteService;
    }

    @GetMapping("/clientes")
    public String clientes(Model model) {

        List<Cliente> clientesL = clienteService.findAll();
        model.addAttribute("clientes", clientesL);

        // Activa el botón "Clientes" en el sidebar
        model.addAttribute("paginaActual", "clientes");

        return "clientes"; // templates/clientes.html
    }

    // 👉 NUEVO CLIENTE
    @GetMapping("/clientes/nuevo")
    public String nuevoCliente(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("esEdicion", false);
        return "nuevo_cliente";
    }

    // 👉 EDITAR CLIENTE (usa el MISMO HTML que nuevo)
    @GetMapping("/clientes/editar/{id}")
    public String editarCliente(@PathVariable Long id, Model model) {
        Cliente cliente = clienteService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));

        model.addAttribute("cliente", cliente);
        model.addAttribute("esEdicion", true);
        return "nuevo_cliente";
    }

    // 👉 GUARDAR (CREA O ACTUALIZA)
    @PostMapping("/clientes/guardar")
    public String guardarCliente(@ModelAttribute Cliente cliente) {
        clienteService.save(cliente);
        return "redirect:/clientes";
    }

    // 👉 ELIMINAR
    @PostMapping("/clientes/eliminar/{id}")
    public String eliminarCliente(@PathVariable Long id) {
        clienteService.deleteById(id);
        return "redirect:/clientes";
    }

}