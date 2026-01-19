package com.example.clientes_venta.Login;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class RegistroController {

    @Autowired
    private UsuarioRepo usuarioRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping(value = "req/signup", consumes = "application/json")
    @Transactional
    public Usuario creaUsuario(@RequestBody Usuario usuario,
                            HttpServletRequest request) {

        // 1) Guardamos password en texto plano para autenticación
        String rawPassword = usuario.getPassword();

        // 2) Encriptamos y guardamos usuario
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        Usuario usuarioGuardado = usuarioRepo.save(usuario);
        entityManager.flush();

        // 3) AUTO LOGIN (clave de todo el problema)
        UsernamePasswordAuthenticationToken authReq =
                new UsernamePasswordAuthenticationToken(
                        usuarioGuardado.getEmail(),
                        rawPassword
                );

        Authentication auth = authenticationManager.authenticate(authReq);

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // 4) Guardar contexto en sesión
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        return usuarioGuardado;
    }

}
