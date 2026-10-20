package com.example.clientes_venta.Usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;



public interface UsuarioRepo extends JpaRepository<Usuario, Integer> {

    void deleteByName(String name);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByName(String name);

    // Administradores de la plataforma (Rol.ADMIN global)
    List<Usuario> findByRol(Rol rol);

    

    
}
