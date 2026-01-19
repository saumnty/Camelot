package com.example.clientes_venta.Login;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.clientes_venta.Usuario.UsuarioRepo;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UsuarioRepo usuarioRepo;

    @ModelAttribute("nombre")
    public String getNombreUsuario(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            var usuario = usuarioRepo.findByEmail(email);
            if (usuario.isPresent()) {
                return usuario.get().getName();
            }
            return email;
        }
        return "Usuario";
    }
}