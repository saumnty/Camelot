package com.example.clientes_venta.Login;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.clientes_venta.Usuario.Rol;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class RegistroController {

    /**
     * Lo único que aceptamos del formulario. Antes se recibía la entidad Usuario completa,
     * así que alguien podía mandar "rol":"ADMIN" y volverse administrador de la plataforma.
     */
    public record RegistroRequest(String name, String email, String password) {}

    private final UsuarioRepo usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public RegistroController(UsuarioRepo usuarioRepo,
                              PasswordEncoder passwordEncoder,
                              AuthenticationManager authenticationManager) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping(value = "req/signup", consumes = "application/json")
    @Transactional
    public ResponseEntity<?> creaUsuario(@RequestBody RegistroRequest req, HttpServletRequest request) {

        String nombre = req.name() == null ? "" : req.name().trim();
        String email = req.email() == null ? "" : req.email().trim();
        String rawPassword = req.password() == null ? "" : req.password();

        if (nombre.isBlank() || email.isBlank() || !email.contains("@")) {
            return ResponseEntity.badRequest().body("Nombre y correo válido son obligatorios.");
        }
        if (rawPassword.length() < 6) {
            return ResponseEntity.badRequest().body("La contraseña debe tener al menos 6 caracteres.");
        }
        if (usuarioRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.status(409).body("Ya existe una cuenta con ese correo.");
        }

        Usuario usuario = new Usuario();
        usuario.setName(nombre);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuario.setRol(Rol.USER); // siempre USER: el rol de plataforma no se elige al registrarse
        Usuario guardado = usuarioRepo.saveAndFlush(usuario);

        // Auto login
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(guardado.getEmail(), rawPassword));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        // No devolvemos la entidad (incluía el hash de la contraseña)
        return ResponseEntity.ok(Map.of("id", guardado.getId(), "name", guardado.getName()));
    }
}
