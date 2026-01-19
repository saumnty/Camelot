package com.example.clientes_venta.Carrito;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Producto.ProductoRepository;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final CarritoItemRepository itemRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepo usuarioRepo;

    public CarritoService(CarritoRepository carritoRepository,
                          CarritoItemRepository itemRepository,
                          ProductoRepository productoRepository,
                          UsuarioRepo usuarioRepo) {
        this.carritoRepository = carritoRepository;
        this.itemRepository = itemRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepo = usuarioRepo;
    }

    @Transactional
    public void agregar(Integer usuarioId, Long productoId) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new IllegalArgumentException("Producto no existe"));

        Carrito carrito = carritoRepository.findByUsuario_IdAndActivoTrue(usuarioId)
            .orElseGet(() -> {
                Carrito c = new Carrito();
                c.setUsuario(usuario);
                c.setActivo(true);
                return carritoRepository.save(c);
            });

        CarritoItem item = carrito.getItems().stream()
            .filter(i -> i.getProducto().getId().equals(productoId))
            .findFirst()
            .orElse(null);

        if (item == null) {
            CarritoItem nuevo = new CarritoItem();
            nuevo.setCarrito(carrito);
            nuevo.setProducto(producto);
            nuevo.setCantidad(1);
            carrito.getItems().add(nuevo);
            itemRepository.save(nuevo);
        } else {
            item.setCantidad(item.getCantidad() + 1);
            itemRepository.save(item);
        }
    }

    public Carrito obtenerCarritoActivo(Integer usuarioId) {
        return carritoRepository.findByUsuario_IdAndActivoTrue(usuarioId).orElse(null);
    }

    @Transactional
    public void incrementar(Long itemId, String email) {
        Usuario u = usuarioRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        CarritoItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Item no existe"));

        // Seguridad: el item debe ser del carrito del usuario
        if (!item.getCarrito().getUsuario().getId().equals(u.getId())) {
            throw new IllegalArgumentException("No autorizado");
        }

        item.setCantidad(item.getCantidad() + 1);
        itemRepository.save(item);
    }

    @Transactional
    public void decrementar(Long itemId, String email) {
        Usuario u = usuarioRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        CarritoItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Item no existe"));

        if (!item.getCarrito().getUsuario().getId().equals(u.getId())) {
            throw new IllegalArgumentException("No autorizado");
        }

        int nueva = item.getCantidad() - 1;
        if (nueva <= 0) {
            itemRepository.delete(item);
        } else {
            item.setCantidad(nueva);
            itemRepository.save(item);
        }
    }

    @Transactional
    public void eliminarItem(Long itemId, String email) {
        Usuario u = usuarioRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        CarritoItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Item no existe"));

        if (!item.getCarrito().getUsuario().getId().equals(u.getId())) {
            throw new IllegalArgumentException("No autorizado");
        }

        itemRepository.delete(item);
    }

    @Transactional
    public void limpiar(String email) {
        Usuario u = usuarioRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));

        Carrito carrito = carritoRepository.findByUsuario_IdAndActivoTrue(u.getId())
            .orElse(null);

        if (carrito != null) {
            carrito.getItems().clear(); // orphanRemoval=true lo borra
            carritoRepository.save(carrito);
        }
    }


}