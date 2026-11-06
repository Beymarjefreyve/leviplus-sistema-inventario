package com.leviplus.ventas;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.repository.LoteRepository;
import com.leviplus.inventario.repository.ProductoRepository;
import com.leviplus.ventas.controller.VentaForm;
import com.leviplus.ventas.model.EstadoVenta;
import com.leviplus.ventas.model.Venta;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VentasTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    VentaService servicio;
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

    Producto acetaminofen;
    Producto ibuprofeno;
    Lote loteProximo;
    Lote loteLejano;
    Lote loteVencido;

    @BeforeEach
    void preparar() {
        limpiar();
        usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        usuarios.save(new Usuario("Beto Ventas", "beto", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS));
        acetaminofen = productos.save(new Producto("V-001", "Acetaminofén", "Caja x 10", "Genfar",
                new BigDecimal("2500.00"), 5, 30));
        ibuprofeno = productos.save(new Producto("V-002", "Ibuprofeno", "Caja x 20", "MK",
                new BigDecimal("4000.00"), 5, 30));
        loteVencido = conExistencia(acetaminofen, "V0", LocalDate.now().minusDays(5), 50);
        loteLejano = conExistencia(acetaminofen, "L2", LocalDate.now().plusDays(90), 10);
        loteProximo = conExistencia(acetaminofen, "L1", LocalDate.now().plusDays(30), 5);
        conExistencia(ibuprofeno, "I1", LocalDate.now().plusDays(60), 3);
    }

    @AfterEach
    void limpiar() {
        ventas.deleteAll();
        lotes.deleteAll();
        productos.deleteAll();
        usuarios.deleteAll();
    }

    private Lote conExistencia(Producto producto, String codigo, LocalDate vence, int cantidad) {
        Lote lote = new Lote(producto, codigo, vence);
        lote.sumar(cantidad);
        return lotes.save(lote);
    }

    private VentaForm form(Object... idYCantidad) {
        VentaForm form = new VentaForm();
        for (int i = 0; i < idYCantidad.length; i += 2) {
            VentaForm.Linea linea = new VentaForm.Linea();
            linea.setIdProducto((Integer) idYCantidad[i]);
            linea.setCantidad((Integer) idYCantidad[i + 1]);
            form.getLineas().add(linea);
        }
        return form;
    }

    private int disponible(Lote lote) {
        return lotes.findById(lote.getIdLote()).orElseThrow().getCantidadDisponible();
    }

    // ---- Registrar venta (RF-08, RF-09)

    @Test
    void ventaDescuentaPrimeroElLoteQueVenceAntesYNuncaElVencido() {
        Venta venta = servicio.registrar(form(acetaminofen.getIdProducto(), 8), "beto");

        assertThat(disponible(loteProximo)).isZero();
        assertThat(disponible(loteLejano)).isEqualTo(7);
        assertThat(disponible(loteVencido)).isEqualTo(50);
        Venta guardada = servicio.obtener(venta.getIdVenta());
        assertThat(guardada.getTotal()).isEqualByComparingTo("20000.00");
        assertThat(guardada.getEstado()).isEqualTo(EstadoVenta.REGISTRADA);
        assertThat(guardada.getDetalles()).hasSize(1);
        assertThat(guardada.getDetalles().get(0).getLotes()).hasSize(2);
    }

    @Test
    void ventaConVariosProductosCalculaElTotalYAcumulaFilasRepetidas() {
        Venta venta = servicio.registrar(form(
                acetaminofen.getIdProducto(), 2, ibuprofeno.getIdProducto(), 1, acetaminofen.getIdProducto(), 1), "ana");

        Venta guardada = servicio.obtener(venta.getIdVenta());
        assertThat(guardada.getDetalles()).hasSize(2);
        assertThat(guardada.getTotal()).isEqualByComparingTo("11500.00");
    }

    @Test
    void sinExistenciaSuficienteNoSeDescuentaNada() {
        assertThatThrownBy(() -> servicio.registrar(form(
                acetaminofen.getIdProducto(), 2, ibuprofeno.getIdProducto(), 99), "beto"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Existencia insuficiente");

        assertThat(disponible(loteProximo)).isEqualTo(5);
        assertThat(ventas.count()).isZero();
    }

    @Test
    void loteVencidoNoCuentaComoExistencia() {
        // Hay 15 unidades vendibles (5 + 10); las 50 del lote vencido no se pueden vender.
        assertThatThrownBy(() -> servicio.registrar(form(acetaminofen.getIdProducto(), 16), "beto"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void productoInactivoNoSePuedeVender() {
        ibuprofeno.inactivar();
        productos.save(ibuprofeno);
        assertThatThrownBy(() -> servicio.registrar(form(ibuprofeno.getIdProducto(), 1), "beto"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void ventaVaciaOCantidadInvalidaSeRechaza() {
        assertThatThrownBy(() -> servicio.registrar(new VentaForm(), "beto"))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> servicio.registrar(form(acetaminofen.getIdProducto(), 0), "beto"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void encargadoDeVentasRegistraVentaYVeElComprobante() throws Exception {
        var resultado = mvc.perform(post("/ventas").with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf())
                        .param("lineas[0].idProducto", acetaminofen.getIdProducto().toString())
                        .param("lineas[0].cantidad", "2")
                        .param("lineas[1].idProducto", "")
                        .param("lineas[1].cantidad", ""))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String destino = resultado.getResponse().getRedirectedUrl();
        assertThat(destino).startsWith("/ventas/");

        mvc.perform(get(destino).with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(status().isOk())
                .andExpect(view().name("ventas/comprobante"));
    }

    @Test
    void formularioMuestraErrorSiNoHayExistencia() throws Exception {
        mvc.perform(post("/ventas").with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf())
                        .param("lineas[0].idProducto", ibuprofeno.getIdProducto().toString())
                        .param("lineas[0].cantidad", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name("ventas/venta-form"))
                .andExpect(model().attributeHasErrors("ventaForm"));
    }

    @Test
    void formularioDeVentaMuestraSoloProductosConExistencia() throws Exception {
        mvc.perform(get("/ventas/nueva").with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(status().isOk())
                .andExpect(view().name("ventas/venta-form"))
                .andExpect(content().string(containsString("Acetaminofén")))
                .andExpect(content().string(containsString("Ibuprofeno")));
        ibuprofeno.inactivar();
        productos.save(ibuprofeno);
        mvc.perform(get("/ventas/nueva").with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(content().string(not(containsString("Ibuprofeno"))));
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/ventas/nueva")).andExpect(status().is3xxRedirection());
    }
}
