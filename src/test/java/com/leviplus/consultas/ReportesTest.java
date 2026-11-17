package com.leviplus.consultas;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.consultas.service.ReporteService;
import com.leviplus.finanzas.model.MovimientoFinanciero;
import com.leviplus.finanzas.model.TipoMovimiento;
import com.leviplus.finanzas.repository.MovimientoFinancieroRepository;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-14 (balance) y RF-16 (reportes de ventas y financieros, CSV). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportesTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ReporteService reportes;
    @Autowired
    VentaService ventasServicio;
    @Autowired
    VentaRepository ventas;
    @Autowired
    DevolucionRepository devoluciones;
    @Autowired
    MovimientoFinancieroRepository movimientos;
    @Autowired
    LoteRepository lotes;
    @Autowired
    ProductoRepository productos;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder codificador;

    Producto producto;
    Usuario ana;
    final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() {
        limpiar();
        ana = usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        usuarios.save(new Usuario("Beto Ventas", "beto", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS));
        producto = productos.save(new Producto("R-001", "Ibuprofeno", "Caja x 20", "MK", new BigDecimal("10000.00"), 5, 30));
        Lote lote = new Lote(producto, "L1", hoy.plusDays(90));
        lote.sumar(100);
        lotes.save(lote);
    }

    @AfterEach
    void limpiar() {
        movimientos.deleteAll();
        devoluciones.deleteAll();
        ventas.deleteAll();
        lotes.deleteAll();
        productos.deleteAll();
        usuarios.deleteAll();
    }

    private Venta vender(int cantidad) {
        VentaForm form = new VentaForm();
        VentaForm.Linea linea = new VentaForm.Linea();
        linea.setIdProducto(producto.getIdProducto());
        linea.setCantidad(cantidad);
        form.getLineas().add(linea);
        return ventasServicio.registrar(form, "beto");
    }

    private void devolver(Venta venta, int cantidad) {
        DevolucionForm form = new DevolucionForm();
        form.setMotivo("Prueba");
        DevolucionForm.Linea linea = new DevolucionForm.Linea();
        linea.setIdDetalleVenta(ventasServicio.obtener(venta.getIdVenta()).getDetalles().get(0).getIdDetalleVenta());
        linea.setCantidad(cantidad);
        linea.setDestino(DestinoDevolucion.REINTEGRO);
        form.getLineas().add(linea);
        ventasServicio.registrarDevolucion(venta.getIdVenta(), form, "ana");
    }

    private void movimiento(TipoMovimiento tipo, String concepto, String valor) {
        movimientos.save(new MovimientoFinanciero(tipo, concepto, null, hoy, new BigDecimal(valor), ana));
    }

    @Test
    void reporteDeVentasExcluyeAnuladasYDescuentaDevoluciones() {
        Venta v1 = vender(3);                              // 30.000
        vender(2);                                         // 20.000
        Venta anulada = vender(5);                         // 50.000, no cuenta
        ventasServicio.anular(anulada.getIdVenta(), "Error de digitación", "ana");
        devolver(v1, 1);                                   // -10.000

        var r = reportes.reporteVentas(hoy, hoy);

        assertThat(r.filas()).hasSize(2);
        assertThat(r.totalBruto()).isEqualByComparingTo("50000");
        assertThat(r.totalDevuelto()).isEqualByComparingTo("10000");
        assertThat(r.totalNeto()).isEqualByComparingTo("40000");
    }

    @Test
    void balanceEsVentasNetasMasOtrosIngresosMenosEgresos() {
        Venta v = vender(4);                               // 40.000
        devolver(v, 1);                                    // neto 30.000
        movimiento(TipoMovimiento.INGRESO, "Servicio de inyectología", "5000");
        movimiento(TipoMovimiento.EGRESO, "Pago a proveedor", "12000");

        var balance = reportes.balance(hoy, hoy);

        assertThat(balance.ventasNetas()).isEqualByComparingTo("30000");
        assertThat(balance.otrosIngresos()).isEqualByComparingTo("5000");
        assertThat(balance.egresos()).isEqualByComparingTo("12000");
        assertThat(balance.balance()).isEqualByComparingTo("23000");
    }

    @Test
    void periodoSinDatosInformaQueNoHayInformacion() throws Exception {
        mvc.perform(get("/reportes").with(user("ana").roles("ADMINISTRADOR"))
                        .param("tipo", "ventas").param("desde", "2020-01-01").param("hasta", "2020-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay información disponible")));
    }

    @Test
    void periodoInvertidoMuestraError() throws Exception {
        mvc.perform(get("/reportes").with(user("ana").roles("ADMINISTRADOR"))
                        .param("desde", "2026-02-01").param("hasta", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no puede ser posterior")));
    }

    @Test
    void exportaVentasACsvCompatibleConExcel() throws Exception {
        vender(2);
        var resultado = mvc.perform(get("/reportes/csv").with(user("ana").roles("ADMINISTRADOR"))
                        .param("tipo", "ventas").param("desde", hoy.toString()).param("hasta", hoy.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(resultado).startsWith("﻿").contains("Venta;Fecha y hora;Atendió;Total;Devuelto;Neto")
                .contains("20000,00");
    }

    @Test
    void exportaReporteFinancieroConBalance() throws Exception {
        movimiento(TipoMovimiento.EGRESO, "Arriendo", "8000");
        var resultado = mvc.perform(get("/reportes/csv").with(user("ana").roles("ADMINISTRADOR"))
                        .param("tipo", "financiero").param("desde", hoy.toString()).param("hasta", hoy.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(resultado).contains("Arriendo").contains("Balance").contains("-8000,00");
    }

    @Test
    void csvNeutralizaFormulasYEscapaSeparadores() {
        movimiento(TipoMovimiento.EGRESO, "=HYPERLINK(\"http://x\")", "100");
        movimiento(TipoMovimiento.EGRESO, "Compra; con punto y coma", "100");

        String csv = reportes.csvFinanciero(reportes.reporteFinanciero(hoy, hoy));

        assertThat(csv).contains("'=HYPERLINK").doesNotContain(";=HYPERLINK").contains("\"Compra; con punto y coma\"");
    }

    @Test
    void soloElAdministradorAccedeALosReportes() throws Exception {
        mvc.perform(get("/reportes").with(user("beto").roles("ENCARGADO_DE_VENTAS"))).andExpect(status().isForbidden());
        mvc.perform(get("/reportes/csv").with(user("beto").roles("ENCARGADO_DE_VENTAS"))
                        .param("tipo", "ventas").param("desde", hoy.toString()).param("hasta", hoy.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reportes")).andExpect(status().is3xxRedirection());
    }
}
