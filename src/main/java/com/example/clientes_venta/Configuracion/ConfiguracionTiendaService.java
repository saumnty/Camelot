package com.example.clientes_venta.Configuracion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaRepository;

@Service
public class ConfiguracionTiendaService {

    private final ConfiguracionTiendaRepository repo;
    private final TiendaRepository tiendaRepo;

    public ConfiguracionTiendaService(ConfiguracionTiendaRepository repo, TiendaRepository tiendaRepo) {
        this.repo = repo;
        this.tiendaRepo = tiendaRepo;
    }

    /**
     * Configuración de la tienda. Si nunca se ha guardado, devuelve los valores por defecto
     * SIN guardarlos (sirve para leer: checkout, tickets, etc.).
     */
    @Transactional(readOnly = true)
    public ConfiguracionTienda de(Long tiendaId) {
        return repo.findByTienda_Id(tiendaId).orElseGet(() -> {
            ConfiguracionTienda c = new ConfiguracionTienda();
            c.setTienda(tiendaRepo.findById(tiendaId).orElse(null));
            return c;
        });
    }

    /** Igual que de(...), pero la deja guardada (para editarla). */
    @Transactional
    public ConfiguracionTienda obtenerOCrear(Long tiendaId) {
        return repo.findByTienda_Id(tiendaId).orElseGet(() -> {
            Tienda t = tiendaRepo.findById(tiendaId)
                    .orElseThrow(() -> new IllegalArgumentException("Tienda no existe: " + tiendaId));
            ConfiguracionTienda c = new ConfiguracionTienda();
            c.setTienda(t);
            return repo.save(c);
        });
    }

    @Transactional
    public ConfiguracionTienda guardar(ConfiguracionTienda c) {
        return repo.save(c);
    }
}
