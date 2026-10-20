package com.example.clientes_venta.Clientes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteTiendaRepository extends JpaRepository<ClienteTienda, Long> {

    Optional<ClienteTienda> findByTienda_IdAndUsuario_Id(Long tiendaId, Integer usuarioId);

    Optional<ClienteTienda> findByIdAndTienda_Id(Long id, Long tiendaId);

    List<ClienteTienda> findByTienda_IdAndEstadoInOrderByCreatedAtDesc(Long tiendaId, Collection<EstadoClienteTienda> estados);

    List<ClienteTienda> findByTienda_IdInAndEstadoOrderByCreatedAtDesc(Collection<Long> tiendaIds, EstadoClienteTienda estado);
}
