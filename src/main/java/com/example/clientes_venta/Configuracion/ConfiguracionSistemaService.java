package com.example.clientes_venta.Configuracion;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfiguracionSistemaService {

    private final ConfiguracionSistemaRepo repo;

    public ConfiguracionSistema obtenerConfiguracion() {
        return repo.findById(1L).orElseGet(() -> {
            ConfiguracionSistema defecto = ConfiguracionSistema.builder()
                    .id(1L)
                    .nombreNegocio("Camelot")
                    .iva(16.0)
                    .moneda("MXN")
                    .autoConfirmarSiHayStock(false)
                    .build();
            return repo.save(defecto);
        });
    }

    public ConfiguracionSistema guardar(ConfiguracionSistema config) {
        config.setId(1L);

        // Normaliza checkbox
        if (config.getAutoConfirmarSiHayStock() == null) {
            config.setAutoConfirmarSiHayStock(false);
        }

        // (Opcional pero recomendado) Limpia strings para evitar espacios raros
        if (config.getNombreNegocio() != null) config.setNombreNegocio(config.getNombreNegocio().trim());
        if (config.getRfc() != null) config.setRfc(config.getRfc().trim());
        if (config.getDireccion() != null) config.setDireccion(config.getDireccion().trim());
        if (config.getTelefono() != null) config.setTelefono(config.getTelefono().trim());
        if (config.getEmailContacto() != null) config.setEmailContacto(config.getEmailContacto().trim());
        if (config.getMoneda() != null) config.setMoneda(config.getMoneda().trim());

        return repo.save(config);
    }
}