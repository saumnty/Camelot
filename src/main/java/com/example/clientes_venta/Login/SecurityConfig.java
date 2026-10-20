package com.example.clientes_venta.Login;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.clientes_venta.Usuario.UsuarioService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UsuarioService usuarioService;

    @Bean
    public UserDetailsService userDetailsService() {
        return usuarioService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // ✅ IMPORTANTÍSIMO: evita HTML “viejo” con token CSRF viejo
            .headers(headers -> headers.cacheControl(Customizer.withDefaults()))

            // ✅ IMPORTANTÍSIMO: al login, migra sesión (evita tokens/sesión desfasados)
            .sessionManagement(sm -> sm.sessionFixation(sf -> sf.migrateSession()))

            .csrf(csrf -> csrf
                // ✅ ok usar cookie (menos propenso a desface por sesión)
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())

                // ❌ NO lo necesitas aquí; puede causar comportamientos raros con thymeleaf/atributos
                // .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())

                // Solo ignora lo que sea realmente público y que no uses con form/CSRF
                .ignoringRequestMatchers("/req/signup")
            )

            // Genera el token CSRF al inicio de CADA petición. Si no, en páginas largas
            // (la barra lateral trae un SVG grande) el token se creaba cuando la respuesta
            // ya se había empezado a enviar, la cookie XSRF-TOKEN nunca llegaba al navegador
            // y el siguiente formulario fallaba con 403 Forbidden.
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)

            .authenticationProvider(authenticationProvider())

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login",
                    "/req/signup",
                    "/error",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/static/**"
                ).permitAll()
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** Fuerza a cargar el token CSRF (y a escribir su cookie) antes de generar la página. */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, java.io.IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
