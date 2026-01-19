package com.example.clientes_venta.Tienda;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Service
public class TiendaProveedorService {

    private final TiendaRepository tiendaRepository;
    private final TiendaProveedorRepository tiendaProveedorRepository;
    private final UsuarioRepo usuarioRepo;
    private final TiendaUsuarioRepository tiendaUsuarioRepo;

    public TiendaProveedorService(
            TiendaRepository tiendaRepository,
            TiendaProveedorRepository tiendaProveedorRepository,
            UsuarioRepo usuarioRepo,
            TiendaUsuarioRepository tiendaUsuarioRepo
    ) {
        this.tiendaRepository = tiendaRepository;
        this.tiendaProveedorRepository = tiendaProveedorRepository;
        this.usuarioRepo = usuarioRepo;
        this.tiendaUsuarioRepo = tiendaUsuarioRepo;
    }

    private Usuario getUsuario(Authentication auth) {
        return usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));
    }

    private void assertAdminDeTienda(Long tiendaId, Usuario u) {
        Integer userId = (u.getId() == null) ? null : u.getId();

        boolean ok = tiendaUsuarioRepo.existsByTienda_IdAndUsuario_IdAndRolAndEstado(
            tiendaId,
            userId,
            RolTienda.ADMIN,
            EstadoMembresia.ACTIVO
        );

        if (!ok) throw new IllegalArgumentException("No eres ADMIN de esta tienda");
    }

    @Transactional
    public void agregarProveedor(Long tiendaId, Long proveedorId, Authentication auth) {
        if (tiendaId.equals(proveedorId)) {
            throw new IllegalArgumentException("Una tienda no puede ser su propio proveedor");
        }

        Usuario u = getUsuario(auth);

        Tienda tienda = tiendaRepository.findById(tiendaId)
                .orElseThrow(() -> new IllegalArgumentException("Tienda compradora no existe"));

        assertAdminDeTienda(tiendaId, u);

        Tienda proveedor = tiendaRepository.findById(proveedorId)
                .orElseThrow(() -> new IllegalArgumentException("Tienda proveedora no existe"));

        if (proveedor.getTipo() == null || proveedor.getTipo() == TipoTienda.NORMAL) {
            throw new IllegalArgumentException("La tienda seleccionada no es PROVEEDOR");
        }

        tiendaProveedorRepository.findByTienda_IdAndProveedor_Id(tiendaId, proveedorId)
                .ifPresentOrElse(tp -> {
                    tp.setActivo(true);
                    tiendaProveedorRepository.save(tp);
                }, () -> {
                    TiendaProveedor tp = new TiendaProveedor(tienda, proveedor);
                    tiendaProveedorRepository.save(tp);
                });
    }

    @Transactional(readOnly = true)
    public List<TiendaProveedor> listarProveedores(Long tiendaId, Authentication auth) {
        Usuario u = getUsuario(auth);

        tiendaRepository.findById(tiendaId)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no existe"));

        assertAdminDeTienda(tiendaId, u);

        return tiendaProveedorRepository.findByTienda_IdAndActivoTrueOrderByCreadoEnDesc(tiendaId);
    }

    @Transactional
    public void quitarProveedor(Long tiendaId, Long proveedorId, Authentication auth) {
        Usuario u = getUsuario(auth);

        tiendaRepository.findById(tiendaId)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no existe"));

        assertAdminDeTienda(tiendaId, u);

        TiendaProveedor tp = tiendaProveedorRepository.findByTienda_IdAndProveedor_Id(tiendaId, proveedorId)
                .orElseThrow(() -> new IllegalArgumentException("Relación tienda-proveedor no existe"));

        tp.setActivo(false);
        tiendaProveedorRepository.save(tp);
    }
}
