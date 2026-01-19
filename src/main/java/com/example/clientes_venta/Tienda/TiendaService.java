package com.example.clientes_venta.Tienda;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Usuario.Usuario;

@Service
public class TiendaService {

    private final TiendaRepository tiendaRepository;
    private final TiendaUsuarioRepository tiendaUsuarioRepo;

    public TiendaService(TiendaRepository tiendaRepository, TiendaUsuarioRepository tiendaUsuarioRepo) {
        this.tiendaRepository = tiendaRepository;
        this.tiendaUsuarioRepo = tiendaUsuarioRepo;
    }

    public List<Tienda> buscar(String q) {
        return tiendaRepository.buscar(q);
    }

    @Transactional
    public Tienda guardarTienda(Tienda tienda) {

        if (tienda.getFechaDeCreacion() == null) {
            tienda.setFechaDeCreacion(LocalDateTime.now());
        }
        if (tienda.getActiva() == null) {
            tienda.setActiva(true);
        }

        if (tienda.getSlug() == null || tienda.getSlug().isBlank()) {
            String slug = tienda.getNombre() == null ? "" : tienda.getNombre()
                    .toLowerCase()
                    .trim()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("(^-|-$)", "");
            tienda.setSlug(slug);
        }

        if (tienda.getGoogleMapsUrl() != null && tienda.getGoogleMapsUrl().isBlank()) {
            tienda.setGoogleMapsUrl(null);
        }

        if (tienda.getTipo() == null) {
            tienda.setTipo(TipoTienda.NORMAL);
        }

        return tiendaRepository.save(tienda);
    }

    /**
     * ✅ Tiendas visibles:
     * - usuario NO logeado => solo NORMALES
     * - usuario logeado:
     *      - si es ADMIN de alguna tienda => NORMALES + PROVEEDOR
     *      - si NO => solo NORMALES
     */
    @Transactional(readOnly = true)
    public List<Tienda> buscarVisibles(String q, Usuario u) {

        if (u == null || u.getId() == null) {
            return tiendaRepository.buscarPorTipos(q, List.of(TipoTienda.NORMAL));
        }

        Integer userId = u.getId(); // ✅ tu Usuario.id es Integer

        boolean esAdminDeAlguna = tiendaUsuarioRepo.existsByUsuario_IdAndRolAndEstado(
                userId,
                RolTienda.ADMIN,
                EstadoMembresia.ACTIVO
        );

        List<TipoTienda> tipos = esAdminDeAlguna
                ? List.of(TipoTienda.NORMAL, TipoTienda.PROVEEDOR)
                : List.of(TipoTienda.NORMAL);

        return tiendaRepository.buscarPorTipos(q, tipos);
    }

    /** ✅ usado por TiendaController */
    @Transactional(readOnly = true)
    public boolean esAdminDeAlgunaTienda(Usuario u) {
        if (u == null || u.getId() == null) return false;

        return tiendaUsuarioRepo.existsByUsuario_IdAndRolAndEstado(
                u.getId(),
                RolTienda.ADMIN,
                EstadoMembresia.ACTIVO
        );
    }

    /** ✅ lista IDs de tiendas donde el usuario es ADMIN */
    @Transactional(readOnly = true)
    public List<Long> tiendasAdminIds(Usuario u) {
        if (u == null || u.getId() == null) return List.of();

        return tiendaUsuarioRepo.findTiendaIdsByUsuarioRolEstado(
                u.getId(),
                RolTienda.ADMIN,
                EstadoMembresia.ACTIVO
        );
    }

    /** ✅ CLAVE: validar si es ADMIN de una tienda específica */
    @Transactional(readOnly = true)
    public boolean esAdminDeTienda(Usuario u, Long tiendaId) {
        if (u == null || u.getId() == null || tiendaId == null) return false;

        return tiendaUsuarioRepo.existsByTienda_IdAndUsuario_IdAndRolAndEstado(
                tiendaId,
                u.getId(),
                RolTienda.ADMIN,
                EstadoMembresia.ACTIVO
        );
    }

    @Transactional
    public void actualizarNombreTienda(Long tiendaId, String nuevoNombre) {

        if (tiendaId == null) {
            throw new IllegalArgumentException("El id de la tienda no puede ser null");
        }

        String limpio = (nuevoNombre == null) ? "" : nuevoNombre.trim();
        if (limpio.isBlank()) {
            throw new IllegalArgumentException("El nombre de la tienda no puede estar vacío");
        }
        if (limpio.length() > 120) {
            limpio = limpio.substring(0, 120);
        }

        String slug = limpio
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        int filas = tiendaRepository.updateNombreYSlug(tiendaId, limpio, slug);

        if (filas == 0) {
            throw new IllegalArgumentException("Tienda no encontrada: " + tiendaId);
        }
    }

    public String obtenerNombreTienda(Long tiendaId) {
        return tiendaRepository.findById(tiendaId)
                .map(Tienda::getNombre)
                .orElse("Tienda");
    }
}
