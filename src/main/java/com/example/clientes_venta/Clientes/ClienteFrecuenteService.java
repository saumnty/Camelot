package com.example.clientes_venta.Clientes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clientes_venta.Configuracion.ConfiguracionTiendaService;
import com.example.clientes_venta.Notificacion.NotificacionService;
import com.example.clientes_venta.Notificacion.TipoNotificacion;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Usuario.Usuario;

/**
 * Flujo de "cliente frecuente" y fiado:
 *   1. El cliente pide ser frecuente desde la página de la tienda → llega al buzón de los admins.
 *   2. Un admin aprueba (con o sin fiado y con un límite) o rechaza desde el buzón.
 *   3. Al pagar, si tiene fiado disponible puede elegir "Fiado". La compra queda PENDIENTE
 *      y se carga a su cuenta hasta que el admin la confirma (autorización personal).
 *   4. El admin registra abonos cuando el cliente paga.
 */
@Service
public class ClienteFrecuenteService {

    private final ClienteTiendaRepository repo;
    private final MovimientoFiadoRepository movRepo;
    private final NotificacionService notificaciones;
    private final ConfiguracionTiendaService configService;

    public ClienteFrecuenteService(ClienteTiendaRepository repo,
                                   MovimientoFiadoRepository movRepo,
                                   NotificacionService notificaciones,
                                   ConfiguracionTiendaService configService) {
        this.repo = repo;
        this.movRepo = movRepo;
        this.notificaciones = notificaciones;
        this.configService = configService;
    }

    public Optional<ClienteTienda> de(Long tiendaId, Usuario u) {
        if (u == null || tiendaId == null) return Optional.empty();
        return repo.findByTienda_IdAndUsuario_Id(tiendaId, u.getId());
    }

    // ---------------------------- Cliente ----------------------------

    @Transactional
    public String solicitar(Tienda tienda, Usuario u, String nota) {
        if (!configService.de(tienda.getId()).isPermiteClientesFrecuentes()) {
            return "Esta tienda no está aceptando clientes frecuentes por ahora.";
        }
        ClienteTienda ct = repo.findByTienda_IdAndUsuario_Id(tienda.getId(), u.getId()).orElse(null);
        if (ct != null) {
            switch (ct.getEstado()) {
                case SOLICITADO: return "Ya enviaste tu solicitud; espera la respuesta de la tienda.";
                case APROBADO:   return "Ya eres cliente frecuente de esta tienda.";
                case SUSPENDIDO: return "Tu cuenta de cliente frecuente está suspendida. Habla con la tienda.";
                case RECHAZADO:  break; // puede volver a intentarlo
            }
        } else {
            ct = new ClienteTienda();
            ct.setTienda(tienda);
            ct.setUsuario(u);
        }
        ct.setEstado(EstadoClienteTienda.SOLICITADO);
        ct.setNotaSolicitud(recortar(nota, 300));
        ct.setCreatedAt(LocalDateTime.now());
        ct.setResueltoEn(null);
        ct.setResueltoPor(null);
        repo.save(ct);

        notificaciones.notificarAdmins(tienda, TipoNotificacion.SOLICITUD_FRECUENTE,
                u.getName() + " quiere ser cliente frecuente",
                (nota == null || nota.isBlank()) ? "Revisa la solicitud y decide si le das fiado." : "Nota: " + nota.trim(),
                "/notificaciones", ct.getId());
        return null; // sin error
    }

    // ----------------------------- Admin -----------------------------

    public List<ClienteTienda> pendientes(Collection<Long> tiendaIds) {
        if (tiendaIds == null || tiendaIds.isEmpty()) return List.of();
        return repo.findByTienda_IdInAndEstadoOrderByCreatedAtDesc(tiendaIds, EstadoClienteTienda.SOLICITADO);
    }

    public List<ClienteTienda> frecuentes(Long tiendaId) {
        return repo.findByTienda_IdAndEstadoInOrderByCreatedAtDesc(tiendaId,
                List.of(EstadoClienteTienda.APROBADO, EstadoClienteTienda.SUSPENDIDO, EstadoClienteTienda.SOLICITADO));
    }

    public List<MovimientoFiado> movimientos(Long clienteTiendaId) {
        return movRepo.findTop20ByClienteTienda_IdOrderByFechaDesc(clienteTiendaId);
    }

    @Transactional
    public void aprobar(Long id, Long tiendaId, Usuario admin, boolean fiado, BigDecimal limite, String notaAdmin) {
        ClienteTienda ct = obtener(id, tiendaId);
        ct.setEstado(EstadoClienteTienda.APROBADO);
        aplicarCredito(ct, fiado, limite);
        ct.setNotaAdmin(recortar(notaAdmin, 300));
        ct.setResueltoEn(LocalDateTime.now());
        ct.setResueltoPor(admin);
        repo.save(ct);

        notificaciones.resolverReferencia(TipoNotificacion.SOLICITUD_FRECUENTE, ct.getId());
        String detalle = ct.isFiadoPermitido()
                ? "Puedes pagar con fiado hasta $" + ct.getLimiteCredito() + "."
                : "Ya apareces como cliente frecuente.";
        notificaciones.notificar(ct.getUsuario(), ct.getTienda(), TipoNotificacion.SOLICITUD_FRECUENTE_RESUELTA,
                "¡Eres cliente frecuente de " + ct.getTienda().getNombre() + "!", detalle,
                "/tiendas/" + ct.getTienda().getId(), ct.getId());
    }

    @Transactional
    public void rechazar(Long id, Long tiendaId, Usuario admin, String notaAdmin) {
        ClienteTienda ct = obtener(id, tiendaId);
        ct.setEstado(EstadoClienteTienda.RECHAZADO);
        ct.setFiadoPermitido(false);
        ct.setNotaAdmin(recortar(notaAdmin, 300));
        ct.setResueltoEn(LocalDateTime.now());
        ct.setResueltoPor(admin);
        repo.save(ct);

        notificaciones.resolverReferencia(TipoNotificacion.SOLICITUD_FRECUENTE, ct.getId());
        notificaciones.notificar(ct.getUsuario(), ct.getTienda(), TipoNotificacion.SOLICITUD_FRECUENTE_RESUELTA,
                "Tu solicitud en " + ct.getTienda().getNombre() + " no fue aprobada",
                notaAdmin == null || notaAdmin.isBlank() ? null : "Motivo: " + notaAdmin.trim(),
                "/tiendas/" + ct.getTienda().getId(), ct.getId());
    }

    @Transactional
    public void actualizarCredito(Long id, Long tiendaId, boolean fiado, BigDecimal limite, boolean suspender) {
        ClienteTienda ct = obtener(id, tiendaId);
        if (ct.getEstado() == EstadoClienteTienda.SOLICITADO || ct.getEstado() == EstadoClienteTienda.RECHAZADO) {
            throw new IllegalArgumentException("Primero aprueba la solicitud desde Notificaciones.");
        }
        ct.setEstado(suspender ? EstadoClienteTienda.SUSPENDIDO : EstadoClienteTienda.APROBADO);
        aplicarCredito(ct, fiado && !suspender, limite);
        repo.save(ct);
    }

    @Transactional
    public void registrarAbono(Long id, Long tiendaId, Usuario admin, BigDecimal monto, String nota) {
        ClienteTienda ct = obtener(id, tiendaId);
        if (monto == null || monto.signum() <= 0) throw new IllegalArgumentException("El abono debe ser mayor a 0.");
        monto = monto.setScale(2, RoundingMode.HALF_UP);
        if (monto.compareTo(ct.getSaldo()) > 0) {
            throw new IllegalArgumentException("El abono ($" + monto + ") es mayor a lo que debe ($" + ct.getSaldo() + ").");
        }
        ct.setSaldo(ct.getSaldo().subtract(monto));
        repo.save(ct);
        guardarMovimiento(ct, MovimientoFiado.Tipo.ABONO, monto, null, nota, admin);

        notificaciones.notificar(ct.getUsuario(), ct.getTienda(), TipoNotificacion.FIADO_MOVIMIENTO,
                "Abono registrado en " + ct.getTienda().getNombre(),
                "Abonaste $" + monto + ". Saldo pendiente: $" + ct.getSaldo() + ".",
                "/tiendas/" + ct.getTienda().getId(), ct.getId());
    }

    // ------------------------- Usado por pagos -------------------------

    /** ¿Puede este usuario fiar este monto en esta tienda? (lo pregunta el checkout) */
    public boolean puedeFiar(Long tiendaId, Usuario u, BigDecimal monto) {
        return de(tiendaId, u)
                .filter(ClienteTienda::puedeFiar)
                .map(ct -> ct.getDisponible().compareTo(monto) >= 0)
                .orElse(false);
    }

    /** Se llama cuando el admin CONFIRMA una compra fiada. */
    @Transactional
    public void cargar(Long tiendaId, Usuario cliente, BigDecimal monto, Long compraId, Usuario admin) {
        ClienteTienda ct = repo.findByTienda_IdAndUsuario_Id(tiendaId, cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("El cliente no es cliente frecuente de esta tienda."));
        if (!ct.puedeFiar()) {
            throw new IllegalArgumentException("Este cliente ya no tiene fiado activo.");
        }
        if (ct.getDisponible().compareTo(monto) < 0) {
            throw new IllegalArgumentException("La compra ($" + monto + ") rebasa el crédito disponible ($"
                    + ct.getDisponible() + "). Sube su límite en Clientes o cancela la compra.");
        }
        ct.setSaldo(ct.getSaldo().add(monto));
        repo.save(ct);
        guardarMovimiento(ct, MovimientoFiado.Tipo.CARGO, monto, compraId, "Compra #" + compraId, admin);
    }

    /** Si se reembolsa/cancela una compra fiada ya cargada, se devuelve a la cuenta. */
    @Transactional
    public void revertirCargo(Long tiendaId, Usuario cliente, BigDecimal monto, Long compraId, Usuario admin) {
        repo.findByTienda_IdAndUsuario_Id(tiendaId, cliente.getId()).ifPresent(ct -> {
            BigDecimal nuevo = ct.getSaldo().subtract(monto);
            ct.setSaldo(nuevo.signum() < 0 ? BigDecimal.ZERO : nuevo);
            repo.save(ct);
            guardarMovimiento(ct, MovimientoFiado.Tipo.ABONO, monto, compraId, "Devolución compra #" + compraId, admin);
        });
    }

    // ---------------------------- helpers ----------------------------

    private ClienteTienda obtener(Long id, Long tiendaId) {
        return repo.findByIdAndTienda_Id(id, tiendaId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado en esta tienda."));
    }

    private static void aplicarCredito(ClienteTienda ct, boolean fiado, BigDecimal limite) {
        if (fiado) {
            if (limite == null || limite.signum() <= 0) {
                throw new IllegalArgumentException("Para dar fiado pon un límite de crédito mayor a 0.");
            }
            ct.setFiadoPermitido(true);
            ct.setLimiteCredito(limite.setScale(2, RoundingMode.HALF_UP));
        } else {
            ct.setFiadoPermitido(false);
            if (limite != null && limite.signum() > 0) ct.setLimiteCredito(limite.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private void guardarMovimiento(ClienteTienda ct, MovimientoFiado.Tipo tipo, BigDecimal monto,
                                   Long compraId, String nota, Usuario quien) {
        MovimientoFiado m = new MovimientoFiado();
        m.setClienteTienda(ct);
        m.setTipo(tipo);
        m.setMonto(monto);
        m.setCompraId(compraId);
        m.setNota(recortar(nota, 255));
        m.setRegistradoPor(quien);
        movRepo.save(m);
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() <= max ? t : t.substring(0, max);
    }
}
