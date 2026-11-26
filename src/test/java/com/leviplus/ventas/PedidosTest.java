package com.leviplus.ventas;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.repository.ProductoRepository;
import com.leviplus.ventas.model.OrigenPedido;
import com.leviplus.ventas.repository.PedidoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** RF-29 y RF-30: registro manual y consulta de pedidos (WhatsApp es solo el origen). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedidosTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    PedidoRepository pedidos;
    @Autowired
    ProductoRepository productos;
    @Autowired
    com.leviplus.inventario.repository.LoteRepository lotes;
    @Autowired
    com.leviplus.ventas.repository.VentaRepository ventas;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder codificador;

    Integer idProducto;

    @BeforeEach
    void preparar() {
        limpiar();
        usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        usuarios.save(new Usuario("Beto Ventas", "beto", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS));
        idProducto = productos.save(new Producto("PED-001", "Acetaminofén", "Caja x 20", "Lab Uno",
                new BigDecimal("5000"), 5, 30)).getIdProducto();
    }

    @AfterEach
    void limpiar() {
        pedidos.deleteAll();
        ventas.deleteAll();
        lotes.deleteAll();
        productos.deleteAll();
        usuarios.deleteAll();
    }

    private MockHttpServletRequestBuilder pedidoValido(String usuario, String rol) {
        return pedido(usuario, rol, "María Pérez", LocalDate.now(), "WHATSAPP", "RECOGE", null);
    }

    private MockHttpServletRequestBuilder pedido(String usuario, String rol, String nombre, LocalDate fecha,
                                                 String origen, String modalidad, String direccion) {
        var req = post("/pedidos").with(user(usuario).roles(rol)).with(csrf())
                .param("nombreCliente", nombre).param("telefono", "3001234567")
                .param("fecha", fecha.toString()).param("origen", origen).param("modalidad", modalidad)
                .param("lineas[0].idProducto", String.valueOf(idProducto)).param("lineas[0].cantidad", "3");
        return direccion == null ? req : req.param("direccion", direccion);
    }

    @Test
    void ambosRolesRegistranUnPedidoDeWhatsAppSinMoverInventario() throws Exception {
        mvc.perform(pedidoValido("ana", "ADMINISTRADOR")).andExpect(status().is3xxRedirection());
        mvc.perform(pedidoValido("beto", "ENCARGADO_DE_VENTAS")).andExpect(status().is3xxRedirection());

        assertThat(pedidos.count()).isEqualTo(2);
        var pedido = pedidos.findAll().get(0);
        assertThat(pedido.getOrigen()).isEqualTo(OrigenPedido.WHATSAPP);
        assertThat(pedido.getDireccion()).isNull();
    }

    @Test
    void pedidoADomicilioExigeDireccion() throws Exception {
        mvc.perform(pedido("ana", "ADMINISTRADOR", "María", LocalDate.now(), "WHATSAPP", "DOMICILIO", null))
                .andExpect(status().isOk())
                .andExpect(view().name("pedidos/pedido-form"))
                .andExpect(content().string(containsString("La dirección es obligatoria")));
        assertThat(pedidos.count()).isZero();

        mvc.perform(pedido("ana", "ADMINISTRADOR", "María", LocalDate.now(), "WHATSAPP", "DOMICILIO", "Calle 10 # 5-20"))
                .andExpect(status().is3xxRedirection());
        assertThat(pedidos.count()).isEqualTo(1);
    }

    @Test
    void pedidoSinProductosOConDatosInvalidosNoSeGuarda() throws Exception {
        mvc.perform(post("/pedidos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombreCliente", " ").param("telefono", "abc")
                        .param("fecha", LocalDate.now().toString()).param("origen", "WHATSAPP")
                        .param("modalidad", "RECOGE"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("pedidoForm", "nombreCliente", "telefono"));

        mvc.perform(post("/pedidos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombreCliente", "María").param("telefono", "3001234567")
                        .param("fecha", LocalDate.now().toString()).param("origen", "LLAMADA")
                        .param("modalidad", "RECOGE"))
                .andExpect(content().string(containsString("Agregue al menos un producto")));
        assertThat(pedidos.count()).isZero();
    }

    @Test
    void noPermiteProductoRepetidoNiFechaFutura() throws Exception {
        mvc.perform(pedidoValido("ana", "ADMINISTRADOR")
                        .param("lineas[1].idProducto", String.valueOf(idProducto)).param("lineas[1].cantidad", "1"))
                .andExpect(content().string(containsString("aparece repetido")));
        mvc.perform(pedido("ana", "ADMINISTRADOR", "María", LocalDate.now().plusDays(2), "WHATSAPP", "RECOGE", null))
                .andExpect(content().string(containsString("no puede ser futura")));
        assertThat(pedidos.count()).isZero();
    }

    @Test
    void consultaConFiltroDeOrigenYDetalle() throws Exception {
        mvc.perform(pedidoValido("beto", "ENCARGADO_DE_VENTAS"));
        mvc.perform(pedido("beto", "ENCARGADO_DE_VENTAS", "Pedro Llamada", LocalDate.now(), "LLAMADA", "RECOGE", null));

        mvc.perform(get("/pedidos").with(user("beto").roles("ENCARGADO_DE_VENTAS")).param("origen", "LLAMADA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pedro Llamada")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("María Pérez"))));

        Integer id = pedidos.findAll().get(0).getIdPedido();
        mvc.perform(get("/pedidos/" + id).with(user("ana").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Acetaminofén")));
    }

    @Test
    void registrarElPedidoComoVentaDescuentaInventarioUnaSolaVez() throws Exception {
        var producto = productos.findById(idProducto).orElseThrow();
        var lote = new com.leviplus.inventario.model.Lote(producto, "L-PED", LocalDate.now().plusDays(60));
        lote.sumar(10);
        lotes.save(lote);
        mvc.perform(pedidoValido("beto", "ENCARGADO_DE_VENTAS"));
        Integer id = pedidos.findAll().get(0).getIdPedido();

        mvc.perform(post("/pedidos/" + id + "/registrar-venta").with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(lotes.findById(lote.getIdLote()).orElseThrow().getCantidadDisponible()).isEqualTo(7);
        assertThat(pedidos.findById(id).orElseThrow().isRegistrado()).isTrue();

        // Segundo intento: no vuelve a descontar.
        mvc.perform(post("/pedidos/" + id + "/registrar-venta").with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(lotes.findById(lote.getIdLote()).orElseThrow().getCantidadDisponible()).isEqualTo(7);
        ventas.deleteAll();
        lotes.deleteAll();
    }

    @Test
    void sinExistenciaElPedidoSigueSinRegistrarse() throws Exception {
        mvc.perform(pedidoValido("beto", "ENCARGADO_DE_VENTAS"));
        Integer id = pedidos.findAll().get(0).getIdPedido();

        mvc.perform(post("/pedidos/" + id + "/registrar-venta").with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(pedidos.findById(id).orElseThrow().isRegistrado()).isFalse();
    }

    @Test
    void sinSesionNoPuedeVerPedidos() throws Exception {
        mvc.perform(get("/pedidos")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/pedidos/nuevo").with(user("x").roles("OTRO"))).andExpect(status().isForbidden());
    }
}
