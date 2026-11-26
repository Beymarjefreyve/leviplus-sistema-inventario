package com.leviplus.finanzas;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.finanzas.model.MovimientoFinanciero;
import com.leviplus.finanzas.model.TipoMovimiento;
import com.leviplus.finanzas.repository.MovimientoFinancieroRepository;
import com.leviplus.inventario.model.Proveedor;
import com.leviplus.inventario.repository.ProveedorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanzasTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    MovimientoFinancieroRepository movimientos;
    @Autowired
    ProveedorRepository proveedores;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder codificador;

    Proveedor distribuidora;
    String hoy = LocalDate.now().toString();

    @BeforeEach
    void preparar() {
        limpiar();
        usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        usuarios.save(new Usuario("Beto Ventas", "beto", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS));
        distribuidora = proveedores.save(new Proveedor("Distribuidora Cúcuta", "3001234567", null));
    }

    @AfterEach
    void limpiar() {
        movimientos.deleteAll();
        proveedores.deleteAll();
        usuarios.deleteAll();
    }

    // ---- Egresos (RF-12)

    @Test
    void egresoPorPagoAProveedorQuedaAsociadoAlProveedor() throws Exception {
        mvc.perform(post("/finanzas/egresos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("idProveedor", String.valueOf(distribuidora.getIdProveedor()))
                        .param("fecha", hoy).param("valor", "250000"))
                .andExpect(redirectedUrl("/finanzas"));

        assertThat(movimientos.findAll()).singleElement().satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimiento.EGRESO);
            assertThat(m.getProveedor().getIdProveedor()).isEqualTo(distribuidora.getIdProveedor());
            assertThat(m.getConcepto()).contains("Distribuidora Cúcuta");
            assertThat(m.getValor()).isEqualByComparingTo("250000");
        });
        assertThat(movimientos.findAllConDetalle().get(0).getUsuario().getNombreUsuario()).isEqualTo("ana");
    }

    @Test
    void egresoPorOtroGastoUsaElConcepto() throws Exception {
        mvc.perform(post("/finanzas/egresos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("concepto", "Pago del arriendo").param("fecha", hoy).param("valor", "900000.50"))
                .andExpect(redirectedUrl("/finanzas"));

        MovimientoFinanciero m = movimientos.findAll().get(0);
        assertThat(m.getProveedor()).isNull();
        assertThat(m.getConcepto()).isEqualTo("Pago del arriendo");
        assertThat(m.getValor()).isEqualByComparingTo("900000.50");
    }

    @Test
    void egresoSinProveedorNiConceptoSeRechaza() throws Exception {
        mvc.perform(post("/finanzas/egresos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("fecha", hoy).param("valor", "1000"))
                .andExpect(status().isOk())
                .andExpect(view().name("finanzas/movimiento-form"));
        assertThat(movimientos.count()).isZero();
    }

    @Test
    void egresoConProveedorInactivoSeRechaza() throws Exception {
        distribuidora.inactivar();
        proveedores.save(distribuidora);
        mvc.perform(post("/finanzas/egresos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("idProveedor", String.valueOf(distribuidora.getIdProveedor()))
                        .param("fecha", hoy).param("valor", "1000"))
                .andExpect(status().isOk())
                .andExpect(view().name("finanzas/movimiento-form"));
        assertThat(movimientos.count()).isZero();
    }

    // ---- Otros ingresos (RF-13)

    @Test
    void administradorRegistraUnIngreso() throws Exception {
        mvc.perform(post("/finanzas/ingresos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("concepto", "Servicio de inyectología").param("fecha", hoy).param("valor", "15000"))
                .andExpect(redirectedUrl("/finanzas"));

        assertThat(movimientos.findAll()).singleElement().satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimiento.INGRESO);
            assertThat(m.getProveedor()).isNull();
            assertThat(m.getValor()).isEqualByComparingTo("15000");
        });
    }

    @Test
    void ingresoSinConceptoSeRechaza() throws Exception {
        mvc.perform(post("/finanzas/ingresos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("fecha", hoy).param("valor", "15000"))
                .andExpect(status().isOk())
                .andExpect(view().name("finanzas/movimiento-form"));
        assertThat(movimientos.count()).isZero();
    }

    @Test
    void valorNoNumericoNegativoOCeroSeRechaza() throws Exception {
        for (String valor : new String[]{"abc", "-5", "0", ""}) {
            mvc.perform(post("/finanzas/ingresos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                            .param("concepto", "Servicio").param("fecha", hoy).param("valor", valor))
                    .andExpect(status().isOk())
                    .andExpect(view().name("finanzas/movimiento-form"));
        }
        assertThat(movimientos.count()).isZero();
    }

    @Test
    void fechaFuturaOFaltanteSeRechaza() throws Exception {
        mvc.perform(post("/finanzas/ingresos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("concepto", "Servicio").param("valor", "100")
                        .param("fecha", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().isOk());
        mvc.perform(post("/finanzas/ingresos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("concepto", "Servicio").param("valor", "100"))
                .andExpect(status().isOk());
        assertThat(movimientos.count()).isZero();
    }

    // ---- Permisos

    @Test
    void encargadoDeVentasNoAccedeALasFinanzas() throws Exception {
        mvc.perform(get("/finanzas").with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/finanzas/ingresos").with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf())
                        .param("concepto", "X").param("fecha", hoy).param("valor", "100"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/finanzas/egresos").with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf())
                        .param("concepto", "X").param("fecha", hoy).param("valor", "100"))
                .andExpect(status().isForbidden());
        assertThat(movimientos.count()).isZero();
    }

    @Test
    void listadoMuestraLosMovimientosRegistrados() throws Exception {
        mvc.perform(post("/finanzas/ingresos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                .param("concepto", "Servicio").param("fecha", hoy).param("valor", "100"));
        mvc.perform(get("/finanzas").with(user("ana").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("finanzas/movimientos"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("Servicio"),
                                org.hamcrest.Matchers.containsString("Ana Admin"))));
    }
}
