package com.leviplus.consultas;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.consultas.service.ConsultaService;
import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.repository.LoteRepository;
import com.leviplus.inventario.repository.ProductoRepository;
import com.leviplus.ventas.controller.VentaForm;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsultasTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ConsultaService consultas;
    @Autowired
    VentaService ventasServicio;
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

    Producto suero;
    Producto gasas;

    @BeforeEach
    void preparar() {
        limpiar();
        usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        usuarios.save(new Usuario("Beto Ventas", "beto", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS));
        // Alerta de vencimiento: 30 días; stock mínimo: 10
        suero = productos.save(new Producto("C-001", "Suero oral", "Sobre 27 g", "Genfar", new BigDecimal("3000.00"), 10, 30));
        gasas = productos.save(new Producto("C-002", "Gasas estériles", "Paquete x 10", "MK", new BigDecimal("1500.00"), 2, 30));
    }

    @AfterEach
    void limpiar() {
        ventas.deleteAll();
        lotes.deleteAll();
        productos.deleteAll();
        usuarios.deleteAll();
    }

    private void lote(Producto producto, String codigo, int diasParaVencer, int cantidad) {
        Lote lote = new Lote(producto, codigo, LocalDate.now().plusDays(diasParaVencer));
        lote.sumar(cantidad);
        lotes.save(lote);
    }

    @Test
    void alertaLotesDentroDelPlazoYLosYaVencidosPeroNoLosLejanos() {
        lote(suero, "A", 10, 5);     // dentro del plazo
        lote(suero, "B", -3, 4);     // ya vencido
        lote(suero, "C", 200, 50);   // lejano
        lote(gasas, "D", 5, 0);      // sin existencia: no alerta

        var alertas = consultas.lotesPorVencer();

        assertThat(alertas).extracting("codigoLote").containsExactly("B", "A");
        assertThat(alertas.get(0).vencido()).isTrue();
        assertThat(alertas.get(1).diasRestantes()).isEqualTo(10);
    }

    @Test
    void bajoStockSoloCuandoLaExistenciaTotalEstaPorDebajoDelMinimo() {
        lote(suero, "A", 100, 6);
        lote(suero, "B", 100, 3);    // total 9 < 10: alerta
        lote(gasas, "G", 100, 2);    // total 2 = mínimo 2: no alerta

        var alertas = consultas.productosBajoStock();

        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).producto()).isEqualTo("Suero oral");
        assertThat(alertas.get(0).existencia()).isEqualTo(9);
    }

    @Test
    void productoSinLotesEstaEnBajoStockYElInactivoNo() {
        gasas.inactivar();
        productos.save(gasas);

        assertThat(consultas.productosBajoStock()).extracting("producto").containsExactly("Suero oral");
    }

    @Test
    void alertasSoloParaAdministrador() throws Exception {
        mvc.perform(get("/alertas").with(user("ana").roles("ADMINISTRADOR")))
                .andExpect(status().isOk()).andExpect(view().name("consultas/alertas"));
        mvc.perform(get("/alertas").with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(status().isForbidden());
    }

}
