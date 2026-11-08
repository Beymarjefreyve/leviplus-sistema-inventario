package com.leviplus.ventas;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.repository.LoteRepository;
import com.leviplus.inventario.repository.ProductoRepository;
import com.leviplus.ventas.controller.DevolucionForm;
import com.leviplus.ventas.controller.VentaForm;
import com.leviplus.ventas.model.DestinoDevolucion;
import com.leviplus.ventas.model.Venta;
import com.leviplus.ventas.repository.DevolucionRepository;
import com.leviplus.ventas.repository.VentaRepository;
import com.leviplus.ventas.service.VentaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DevolucionesTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    VentaService servicio;
    @Autowired
    DevolucionRepository devoluciones;
    @Autowired
    VentaRepository ventas;
    @Autowired
    LoteRepository lotes;
    @Autowired
    ProductoRepository productos;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder codificador;

    Producto crema;
    Lote lote1;
    Lote lote2;

    @BeforeEach
    void preparar() {
        limpiar();
        usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        usuarios.save(new Usuario("Beto Ventas", "beto", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS));
        crema = productos.save(new Producto("D-001", "Crema hidratante", "Tubo 50 g", "Eucerin",
                new BigDecimal("10000.00"), 1, 30));
        lote1 = lote("A", 20, 3);
        lote2 = lote("B", 60, 10);
    }

    @AfterEach
    void limpiar() {
        devoluciones.deleteAll();
        ventas.deleteAll();
        lotes.deleteAll();
        productos.deleteAll();
        usuarios.deleteAll();
    }

    private Lote lote(String codigo, int dias, int cantidad) {
        Lote l = new Lote(crema, codigo, LocalDate.now().plusDays(dias));
        l.sumar(cantidad);
        return lotes.save(l);
    }

    private int disponible(Lote l) {
        return lotes.findById(l.getIdLote()).orElseThrow().getCantidadDisponible();
    }

    private Venta vender(int cantidad) {
        VentaForm form = new VentaForm();
        VentaForm.Linea linea = new VentaForm.Linea();
        linea.setIdProducto(crema.getIdProducto());
        linea.setCantidad(cantidad);
        form.getLineas().add(linea);
        return servicio.registrar(form, "beto");
    }

    private DevolucionForm devolucion(Venta venta, int cantidad, DestinoDevolucion destino, String motivo) {
        DevolucionForm form = new DevolucionForm();
        form.setMotivo(motivo);
        DevolucionForm.Linea linea = new DevolucionForm.Linea();
        linea.setIdDetalleVenta(servicio.obtener(venta.getIdVenta()).getDetalles().get(0).getIdDetalleVenta());
        linea.setCantidad(cantidad);
        linea.setDestino(destino);
        form.getLineas().add(linea);
        return form;
    }

    // ---- Devoluciones (RF-26)

    @Test
    void devolucionConReintegroVuelveAlLoteDeOrigenYDescuentaElValor() {
        Venta venta = vender(5);                       // 3 de A + 2 de B
        assertThat(disponible(lote1)).isZero();

        var dev = servicio.registrarDevolucion(venta.getIdVenta(), devolucion(venta, 4, DestinoDevolucion.REINTEGRO, "Cambio de opinión"), "ana");

        assertThat(dev.getValorDevuelto()).isEqualByComparingTo("40000.00");
        assertThat(disponible(lote1)).isEqualTo(3);    // las 3 unidades de A vuelven a A
        assertThat(disponible(lote2)).isEqualTo(10 - 2 + 1);
        assertThat(servicio.devolucionesDe(venta.getIdVenta())).hasSize(1);
    }

    @Test
    void devolucionConBajaNoTocaElInventario() {
        Venta venta = vender(2);
        int antes = disponible(lote1);

        servicio.registrarDevolucion(venta.getIdVenta(), devolucion(venta, 2, DestinoDevolucion.BAJA, "Producto dañado"), "ana");

        assertThat(disponible(lote1)).isEqualTo(antes);
    }

    @Test
    void noSePuedeDevolverMasDeLoVendidoNiRepetirLoYaDevuelto() {
        Venta venta = vender(3);
        assertThatThrownBy(() -> servicio.registrarDevolucion(venta.getIdVenta(),
                devolucion(venta, 4, DestinoDevolucion.REINTEGRO, "x"), "ana"))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("excede");

        servicio.registrarDevolucion(venta.getIdVenta(), devolucion(venta, 2, DestinoDevolucion.REINTEGRO, "x"), "ana");
        assertThatThrownBy(() -> servicio.registrarDevolucion(venta.getIdVenta(),
                devolucion(venta, 2, DestinoDevolucion.REINTEGRO, "x"), "ana"))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("excede");
        assertThat(servicio.devolvibles(servicio.obtener(venta.getIdVenta())).values()).containsExactly(1L);
    }

    @Test
    void devolucionExigeMotivoYAlMenosUnaCantidad() {
        Venta venta = vender(1);
        assertThatThrownBy(() -> servicio.registrarDevolucion(venta.getIdVenta(),
                devolucion(venta, 1, DestinoDevolucion.REINTEGRO, " "), "ana"))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("motivo");
        assertThatThrownBy(() -> servicio.registrarDevolucion(venta.getIdVenta(),
                devolucion(venta, 0, DestinoDevolucion.REINTEGRO, "x"), "ana"))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("al menos un producto");
    }

    @Test
    void ventaAnuladaNoAdmiteDevolucionYAnularConDevolucionRestituyeSoloLoPendiente() {
        Venta venta = vender(5);
        servicio.registrarDevolucion(venta.getIdVenta(), devolucion(venta, 4, DestinoDevolucion.REINTEGRO, "x"), "ana");

        servicio.anular(venta.getIdVenta(), "Error", "ana");

        // Stock original: A=3, B=10. Tras anular, todo vuelve exactamente a lo original (sin duplicar lo devuelto).
        assertThat(disponible(lote1)).isEqualTo(3);
        assertThat(disponible(lote2)).isEqualTo(10);
        assertThatThrownBy(() -> servicio.registrarDevolucion(venta.getIdVenta(),
                devolucion(venta, 1, DestinoDevolucion.REINTEGRO, "x"), "ana"))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("anulada");
    }

    @Test
    void anularTrasDevolverConBajaNoResucitaLoDadoDeBaja() {
        Venta venta = vender(3);
        servicio.registrarDevolucion(venta.getIdVenta(), devolucion(venta, 3, DestinoDevolucion.BAJA, "Dañado"), "ana");

        servicio.anular(venta.getIdVenta(), "Error", "ana");

        assertThat(disponible(lote1)).isZero();
    }

    @Test
    void masVendidosDescuentaLasUnidadesDevueltas() {
        Venta venta = vender(5);
        servicio.registrarDevolucion(venta.getIdVenta(), devolucion(venta, 2, DestinoDevolucion.REINTEGRO, "x"), "ana");

        var ranking = servicio.masVendidos(LocalDate.now(), LocalDate.now());

        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).unidades()).isEqualTo(3);
    }

    @Test
    void soloElAdministradorRegistraDevoluciones() throws Exception {
        Venta venta = vender(1);
        mvc.perform(get("/ventas/" + venta.getIdVenta() + "/devolucion").with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/ventas/" + venta.getIdVenta() + "/devoluciones").with(user("beto").roles("ENCARGADO_DE_VENTAS"))
                        .with(csrf()).param("motivo", "x"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/ventas/" + venta.getIdVenta() + "/devolucion").with(user("ana").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crema hidratante")));
    }

    @Test
    void administradorRegistraLaDevolucionDesdeElFormulario() throws Exception {
        Venta venta = vender(2);
        Integer idDetalle = servicio.obtener(venta.getIdVenta()).getDetalles().get(0).getIdDetalleVenta();

        mvc.perform(post("/ventas/" + venta.getIdVenta() + "/devoluciones").with(user("ana").roles("ADMINISTRADOR"))
                        .with(csrf())
                        .param("motivo", "Vencimiento cercano")
                        .param("lineas[0].idDetalleVenta", idDetalle.toString())
                        .param("lineas[0].cantidad", "1")
                        .param("lineas[0].destino", "BAJA"))
                .andExpect(status().is3xxRedirection());

        assertThat(servicio.devolucionesDe(venta.getIdVenta())).hasSize(1);
        mvc.perform(get("/ventas/" + venta.getIdVenta()).with(user("ana").roles("ADMINISTRADOR")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dar de baja")));
    }
}
