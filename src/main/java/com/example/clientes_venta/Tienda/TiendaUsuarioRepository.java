package com.example.clientes_venta.Tienda;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TiendaUsuarioRepository extends JpaRepository<TiendaUsuario, Long> {

    // ======= EXISTENTES (NO TOCAR) =======
    boolean existsByTienda_IdAndUsuario_IdAndRolAndEstado(
            Long tiendaId,
            Integer usuarioId,
            RolTienda rol,
            EstadoMembresia estado
    );

    boolean existsByUsuario_IdAndRolAndEstado(
            Integer usuarioId,
            RolTienda rol,
            EstadoMembresia estado
    );

    @Query("""
        select tu.tienda.id
        from TiendaUsuario tu
        where tu.usuario.id = :usuarioId
          and tu.rol = :rol
          and tu.estado = :estado
    """)
    List<Long> findTiendaIdsByUsuarioRolEstado(
            @Param("usuarioId") Integer usuarioId,
            @Param("rol") RolTienda rol,
            @Param("estado") EstadoMembresia estado
    );

    // ======= EXISTENTE (pero estaba mal tipado en tu repo) =======
    // OJO: lo correcto en tu proyecto es Integer (porque Usuario.id es Integer)
    @Query("""
        SELECT t.nombre
        FROM TiendaUsuario tu
        JOIN tu.tienda t
        WHERE tu.usuario.id = :usuarioId
          AND tu.rol = 'CLIENTE'
        ORDER BY tu.id DESC
    """)
    Optional<String> nombreTiendaAsignadaCliente(@Param("usuarioId") Integer usuarioId);

    // ======= NUEVOS (AGREGADOS) =======
    // 1) Misma idea pero filtrando por rol+estado (para tu caso "CLIENTE" + "ACTIVO")
    @Query("""
        SELECT t.nombre
        FROM TiendaUsuario tu
        JOIN tu.tienda t
        WHERE tu.usuario.id = :usuarioId
          AND tu.rol = :rol
          AND tu.estado = :estado
        ORDER BY tu.id DESC
    """)
    Optional<String> nombreTiendaAsignadaCliente(
            @Param("usuarioId") Integer usuarioId,
            @Param("rol") RolTienda rol,
            @Param("estado") EstadoMembresia estado
    );

    // 2) Overloads por si en alguna parte manejas usuarioId como Long (sin romper nada)
    boolean existsByTienda_IdAndUsuario_IdAndRolAndEstado(
            Long tiendaId,
            Long usuarioId,
            RolTienda rol,
            EstadoMembresia estado
    );

    boolean existsByUsuario_IdAndRolAndEstado(
            Long usuarioId,
            RolTienda rol,
            EstadoMembresia estado
    );

    @Query("""
        select tu.tienda.id
        from TiendaUsuario tu
        where tu.usuario.id = :usuarioId
          and tu.rol = :rol
          and tu.estado = :estado
    """)
    List<Long> findTiendaIdsByUsuarioRolEstado(
            @Param("usuarioId") Long usuarioId,
            @Param("rol") RolTienda rol,
            @Param("estado") EstadoMembresia estado
    );

    @Query("""
        SELECT t.nombre
        FROM TiendaUsuario tu
        JOIN tu.tienda t
        WHERE tu.usuario.id = :usuarioId
          AND tu.rol = 'CLIENTE'
        ORDER BY tu.id DESC
    """)
    Optional<String> nombreTiendaAsignadaCliente(@Param("usuarioId") Long usuarioId);

    @Query("""
        SELECT t.nombre
        FROM TiendaUsuario tu
        JOIN tu.tienda t
        WHERE tu.usuario.id = :usuarioId
          AND tu.rol = :rol
          AND tu.estado = :estado
        ORDER BY tu.id DESC
    """)
    Optional<String> nombreTiendaAsignadaCliente(
            @Param("usuarioId") Long usuarioId,
            @Param("rol") RolTienda rol,
            @Param("estado") EstadoMembresia estado
    );

    @Query("""
        SELECT t.nombre
        FROM TiendaUsuario tu
        JOIN tu.tienda t
        WHERE tu.usuario.id = :usuarioId
        AND tu.rol = 'ADMIN'
        AND tu.estado = 'ACTIVO'
        AND t.tipo = 'NORMAL'
        ORDER BY tu.id DESC
    """)
    Optional<String> nombreTiendaNormalAdminDeUsuario(@Param("usuarioId") Integer usuarioId);

    Optional<TiendaUsuario> findByTienda_IdAndUsuario_Id(Long tiendaId, Integer usuarioId);

    boolean existsByTienda_IdAndUsuario_IdAndRol(
                Long tiendaId,
                Integer usuarioId,
                RolTienda rol
        );

        boolean existsByUsuarioIdAndTiendaIdAndRolAndEstado(
                Integer usuarioId,
                Long tiendaId,
                RolTienda rol,
                EstadoMembresia estado
                );

}
