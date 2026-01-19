package com.example.clientes_venta.Usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UsuarioService implements UserDetailsService{

    private final UsuarioRepo usuarioRepo;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{

        Optional<Usuario> user = usuarioRepo.findByEmail(email);

        if (user.isPresent()) {
            var userObj = user.get();
            return User.builder()
                    .username(userObj.getEmail())
                    .password(userObj.getPassword())
                    .roles(userObj.getAuthorities().stream()
                            .map(a -> a.getAuthority())
                            .toArray(String[]::new))
                    .build();
        } else {
            throw new UsernameNotFoundException(email);
        }
    }

    @Autowired
    public UsuarioService(UsuarioRepo usuarioRepo){
        this.usuarioRepo=usuarioRepo;
    }

    public List<Usuario> getUsuarios(){ 
        return usuarioRepo.findAll();
    }

}
