package com.example.clientes_venta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.clientes_venta.Carrito.CarritoService;
import com.example.clientes_venta.Clientes.ClienteTienda;
import com.example.clientes_venta.Clientes.ClienteTiendaRepository;
import com.example.clientes_venta.Clientes.EstadoClienteTienda;
import com.example.clientes_venta.Compra.Compra;
import com.example.clientes_venta.Compra.CompraRepository;
import com.example.clientes_venta.Compra.EstadoCompra;
import com.example.clientes_venta.Configuracion.ConfiguracionTienda;
import com.example.clientes_venta.Configuracion.ConfiguracionTiendaService;
import com.example.clientes_venta.Pago.EstadoPago;
import com.example.clientes_venta.Pago.MetodoPago;
import com.example.clientes_venta.Producto.Producto;
import com.example.clientes_venta.Producto.ProductoRepository;
import com.example.clientes_venta.Tienda.Tienda;
import com.example.clientes_venta.Tienda.TiendaService;
import com.example.clientes_venta.Tienda.TipoTienda;
import com.example.clientes_venta.Usuario.Rol;
import com.example.clientes_venta.Usuario.Usuario;
import com.example.clientes_venta.Usuario.UsuarioRepo;

/**
 * Prueba de punta a punta contra una base Postgres real (la que levanta GitHub Actions).
 * Revisa que las páginas se dibujen (Thymeleaf) y que los flujos nuevos funcionen:
 * barra lateral por rol, configuración por tienda, pago simulado, fiado y reembolso.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlujosPrincipalesTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepo usuarioRepo;
    @Autowired TiendaService tiendaService;
    @Autowired ProductoRepository productoRepo;
    @Autowired CarritoService carritoService;
    @Autowired CompraRepository compraRepo;
    @Autowired ConfiguracionTiendaService configService;
    @Autowired ClienteTiendaRepository clienteTiendaRepo;

    Usuario dueno;
    Usuario cliente;
    Tienda tienda;
    Producto refresco;

    @BeforeEach
    void datos() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        dueno = nuevoUsuario("Dueño " + s, "dueno-" + s + "@test.mx");
        cliente = nuevoUsuario("Cliente " + s, "cliente-" + s + "@test.mx");

        Tienda t = new Tienda();
        t.setNombre("Abarrotes Prueba " + s);
        t.setTipo(TipoTienda.NORMAL);
        t.setOwner(dueno);
        tienda = tiendaService.guardarTienda(t); // el dueño queda como ADMIN

        Producto p = new Producto();
        p.setName("Refresco 600ml");
        p.setPrecio(new BigDecimal("20.00"));
        p.setStock(10);
        p.setTienda(tienda);
        refresco = productoRepo.save(p);
    }

    private Usuario nuevoUsuario(String nombre, String email) {
        Usuario u = new Usuario();
        u.setName(nombre);
        u.setEmail(email);
        u.setPassword("{noop}x");
        u.setRol(Rol.USER);
        return usuarioRepo.save(u);
    }

    private static RequestPostProcessor como(Usuario u) {
        return user(u.getEmail()).roles("USER");
    }

    // ------------------------------------------------------------------ barra lateral

    @Test
    void clienteNoVeSeccionMiTienda() throws Exception {
        mvc.perform(get("/home").with(como(cliente)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mis compras")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Configuración de la tienda"))));
    }

    @Test
    void duenoVeSeccionMiTienda() throws Exception {
        mvc.perform(get("/home").with(como(dueno)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Configuración de la tienda")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mis compras")));
    }

    @Test
    void clienteDentroDeTiendaVeSubmenu() throws Exception {
        mvc.perform(get("/tiendas/" + tienda.getId()).with(como(cliente)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mi carrito aquí")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Soy cliente frecuente")));
    }

    // ------------------------------------------------------------------ páginas

    @Test
    void paginasDelClienteSeDibujan() throws Exception {
        carritoService.agregar(cliente.getId(), refresco.getId());
        for (String url : List.of("/home", "/tiendas/" + tienda.getId(), "/productos?tiendaId=" + tienda.getId(),
                "/carrito", "/carrito?tiendaId=" + tienda.getId(), "/checkout", "/compras",
                "/notificaciones", "/config/cuenta")) {
            mvc.perform(get(url).with(como(cliente))).andExpect(status().isOk());
        }
    }

    @Test
    void paginasDelDuenoSeDibujan() throws Exception {
        Long id = tienda.getId();
        for (String url : List.of("/productos?tiendaId=" + id, "/ventas?tiendaId=" + id, "/ventas/historial?tiendaId=" + id,
                "/clientes?tiendaId=" + id, "/clientes?tiendaId=" + id + "&tab=frecuentes",
                "/clientes?tiendaId=" + id + "&tab=libreta", "/config/tienda?tiendaId=" + id,
                "/admin/compras-proveedor?tiendaId=" + id, "/admin/compra-externa?tiendaId=" + id,
                "/admin/reportes/ventas?tiendaId=" + id, "/admin/reportes/resumen-diario?tiendaId=" + id,
                "/camaras?tiendaId=" + id, "/notificaciones")) {
            mvc.perform(get(url).with(como(dueno))).andExpect(status().isOk());
        }
    }

    @Test
    void clienteNoPuedeAbrirNiGuardarConfiguracionDeTienda() throws Exception {
        mvc.perform(get("/config/tienda?tiendaId=" + tienda.getId()).with(como(cliente)))
                .andExpect(redirectedUrl("/config/cuenta"));
        mvc.perform(post("/config/tienda").with(como(cliente)).with(csrf())
                        .param("tiendaId", tienda.getId().toString())
                        .param("nombre", "Hackeada").param("iva", "16").param("moneda", "MXN"))
                .andExpect(redirectedUrl("/config/cuenta"));
        assertThat(tiendaService.obtenerNombreTienda(tienda.getId())).doesNotContain("Hackeada");
    }

    // ------------------------------------------------------------------ pago simulado

    @Test
    void pagoConTarjetaYReembolso() throws Exception {
        carritoService.agregar(cliente.getId(), refresco.getId());

        mvc.perform(post("/checkout").with(como(cliente)).with(csrf())
                        .param("metodo_" + tienda.getId(), "TARJETA")
                        .param("tarjetaNumero", "4242 4242 4242 4242")
                        .param("tarjetaTitular", "Cliente Prueba")
                        .param("tarjetaVencimiento", "12/39")
                        .param("tarjetaCvv", "123"))
                .andExpect(redirectedUrl("/compras"));

        Compra c = compraRepo.findByUsuario_IdOrderByFechaDesc(cliente.getId()).get(0);
        assertThat(c.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(c.getEstado()).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(c.getTotal()).isEqualByComparingTo("20.00");

        // El dueño ve el ticket y cancela → se reembolsa
        mvc.perform(get("/compras/ticket/" + c.getId()).with(como(dueno))).andExpect(status().isOk());
        mvc.perform(post("/ventas/" + c.getId() + "/cancelar").with(como(dueno)).with(csrf())
                        .param("tiendaId", tienda.getId().toString()).param("motivo", "Sin existencias"))
                .andExpect(status().is3xxRedirection());

        Compra despues = compraRepo.findById(c.getId()).orElseThrow();
        assertThat(despues.getEstado()).isEqualTo(EstadoCompra.REEMBOLSADA);
        assertThat(despues.getEstadoPago()).isEqualTo(EstadoPago.REEMBOLSADO);
    }

    @Test
    void tarjetaRechazadaNoCreaPedido() throws Exception {
        carritoService.agregar(cliente.getId(), refresco.getId());
        int antes = compraRepo.findByUsuario_IdOrderByFechaDesc(cliente.getId()).size();

        mvc.perform(post("/checkout").with(como(cliente)).with(csrf())
                        .param("metodo_" + tienda.getId(), "TARJETA")
                        .param("tarjetaNumero", "4000 0000 0000 0002")
                        .param("tarjetaTitular", "Cliente Prueba")
                        .param("tarjetaVencimiento", "12/39")
                        .param("tarjetaCvv", "123"))
                .andExpect(redirectedUrl("/checkout"));

        assertThat(compraRepo.findByUsuario_IdOrderByFechaDesc(cliente.getId())).hasSize(antes);
    }

    @Test
    void efectivoSoloSiLaTiendaLoPermite() throws Exception {
        ConfiguracionTienda cfg = configService.obtenerOCrear(tienda.getId());
        cfg.setAceptaEfectivo(false);
        configService.guardar(cfg);
        carritoService.agregar(cliente.getId(), refresco.getId());

        mvc.perform(post("/checkout").with(como(cliente)).with(csrf())
                        .param("metodo_" + tienda.getId(), "EFECTIVO"))
                .andExpect(redirectedUrl("/checkout"));
    }

    // ------------------------------------------------------------------ cliente frecuente + fiado

    @Test
    void clienteFrecuenteConFiado() throws Exception {
        ConfiguracionTienda cfg = configService.obtenerOCrear(tienda.getId());
        cfg.setAceptaFiado(true);
        configService.guardar(cfg);

        // 1) El cliente lo pide
        mvc.perform(post("/tiendas/" + tienda.getId() + "/frecuente").with(como(cliente)).with(csrf())
                        .param("nota", "Soy vecino"))
                .andExpect(redirectedUrl("/tiendas/" + tienda.getId()));
        ClienteTienda ct = clienteTiendaRepo.findByTienda_IdAndUsuario_Id(tienda.getId(), cliente.getId()).orElseThrow();
        assertThat(ct.getEstado()).isEqualTo(EstadoClienteTienda.SOLICITADO);

        // 2) Le llega al dueño en Notificaciones y lo aprueba con $100 de fiado
        mvc.perform(get("/notificaciones").with(como(dueno)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(cliente.getName())));
        mvc.perform(post("/clientes/frecuentes/" + ct.getId() + "/aprobar").with(como(dueno)).with(csrf())
                        .param("tiendaId", tienda.getId().toString())
                        .param("fiado", "true").param("limite", "100"))
                .andExpect(redirectedUrl("/notificaciones"));

        // 3) El cliente compra a fiado → queda pendiente
        carritoService.agregar(cliente.getId(), refresco.getId());
        mvc.perform(post("/checkout").with(como(cliente)).with(csrf())
                        .param("metodo_" + tienda.getId(), MetodoPago.FIADO.name()))
                .andExpect(redirectedUrl("/compras"));
        Compra c = compraRepo.findByUsuario_IdOrderByFechaDesc(cliente.getId()).get(0);
        assertThat(c.getMetodoPago()).isEqualTo(MetodoPago.FIADO);

        // El pedido aparece en el buzón del dueño con botón para aceptarlo
        mvc.perform(get("/notificaciones").with(como(dueno)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/ventas/" + c.getId() + "/aprobar")));
        assertThat(c.getEstado()).isEqualTo(EstadoCompra.PENDIENTE);

        // 4) El dueño confirma → se carga a la cuenta y baja el stock
        mvc.perform(post("/ventas/" + c.getId() + "/aprobar").with(como(dueno)).with(csrf())
                        .param("tiendaId", tienda.getId().toString()).param("volver", "buzon"))
                .andExpect(redirectedUrl("/notificaciones?tiendaId=" + tienda.getId()));
        assertThat(compraRepo.findById(c.getId()).orElseThrow().getEstadoPago()).isEqualTo(EstadoPago.A_CUENTA);
        assertThat(clienteTiendaRepo.findById(ct.getId()).orElseThrow().getSaldo()).isEqualByComparingTo("20.00");
        assertThat(productoRepo.findById(refresco.getId()).orElseThrow().getStock()).isEqualTo(9);

        // 5) Abono
        mvc.perform(post("/clientes/frecuentes/" + ct.getId() + "/abono").with(como(dueno)).with(csrf())
                        .param("tiendaId", tienda.getId().toString()).param("monto", "20"))
                .andExpect(status().is3xxRedirection());
        assertThat(clienteTiendaRepo.findById(ct.getId()).orElseThrow().getSaldo()).isEqualByComparingTo("0.00");
    }

    @Test
    void otroDuenoNoPuedeAprobarVentasAjenas() throws Exception {
        carritoService.agregar(cliente.getId(), refresco.getId());
        mvc.perform(post("/checkout").with(como(cliente)).with(csrf())
                        .param("metodo_" + tienda.getId(), "EFECTIVO"))
                .andExpect(redirectedUrl("/compras"));
        Compra c = compraRepo.findByUsuario_IdOrderByFechaDesc(cliente.getId()).get(0);

        // Un dueño de OTRA tienda intenta confirmarla pasando su propio tiendaId
        Usuario intruso = nuevoUsuario("Intruso", "intruso-" + UUID.randomUUID().toString().substring(0, 8) + "@test.mx");
        Tienda suya = new Tienda();
        suya.setNombre("Otra tienda");
        suya.setOwner(intruso);
        suya = tiendaService.guardarTienda(suya);

        mvc.perform(post("/ventas/" + c.getId() + "/aprobar").with(como(intruso)).with(csrf())
                        .param("tiendaId", suya.getId().toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(compraRepo.findById(c.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCompra.PENDIENTE);
    }

    @Test
    void registroNoPermiteVolverseAdmin() throws Exception {
        String email = "nuevo-" + UUID.randomUUID().toString().substring(0, 8) + "@test.mx";
        mvc.perform(post("/req/signup").contentType("application/json")
                        .content("{\"name\":\"Nuevo\",\"email\":\"" + email + "\",\"password\":\"secreto1\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))));
        assertThat(usuarioRepo.findByEmail(email).orElseThrow().getRol()).isEqualTo(Rol.USER);
    }
}
