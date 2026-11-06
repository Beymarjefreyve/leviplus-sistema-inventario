package com.leviplus.inventario;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.model.Proveedor;
import com.leviplus.inventario.repository.EntradaInventarioRepository;
import com.leviplus.inventario.repository.LoteRepository;
import com.leviplus.inventario.repository.ProductoRepository;
import com.leviplus.inventario.repository.ProveedorRepository;
import com.leviplus.inventario.repository.SalidaInventarioRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventarioTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    ProductoRepository productos;
    @Autowired
    ProveedorRepository proveedores;
    @Autowired
    LoteRepository lotes;
    @Autowired
    EntradaInventarioRepository entradas;
    @Autowired
    SalidaInventarioRepository salidas;
    @Autowired
    PasswordEncoder codificador;

    Producto paracetamol;
    Proveedor distribuidora;
    String vence = LocalDate.now().plusYears(1).toString();

    @BeforeEach
    void preparar() {
        limpiar();
        usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        usuarios.save(new Usuario("Beto Ventas", "beto", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS));
        paracetamol = productos.save(new Producto("P-001", "Paracetamol 500 mg", "Caja x 10", "Genfar",
                new BigDecimal("3500.00"), 5, 30));
        distribuidora = proveedores.save(new Proveedor("Distribuidora Cúcuta", "3001234567", null));
    }

    @AfterEach
    void limpiar() {
        salidas.deleteAll();
        entradas.deleteAll();
        lotes.deleteAll();
        productos.deleteAll();
        proveedores.deleteAll();
        usuarios.deleteAll();
    }

    private long existencia() {
        return lotes.existenciaDe(productos.findById(paracetamol.getIdProducto()).orElseThrow());
    }

    private void entrada(String lote, String vencimiento, String cantidad) throws Exception {
        mvc.perform(post("/productos/{id}/entrada", paracetamol.getIdProducto())
                .with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                .param("idProveedor", String.valueOf(distribuidora.getIdProveedor()))
                .param("codigoLote", lote)
                .param("fechaVencimiento", vencimiento)
                .param("cantidad", cantidad));
    }

    // ---- Entradas por lote (RF-04)

    @Test
    void entradaCreaElLoteYSumaLaExistencia() throws Exception {
        entrada("L-1", vence, "40");
        assertThat(existencia()).isEqualTo(40);
        Lote lote = lotes.findByProductoAndCodigoLote(paracetamol, "L-1").orElseThrow();
        assertThat(lote.getFechaVencimiento()).isEqualTo(LocalDate.parse(vence));
        assertThat(entradas.count()).isEqualTo(1);
    }

    @Test
    void segundaEntradaDelMismoLoteSumaAlLoteExistente() throws Exception {
        entrada("L-1", vence, "40");
        entrada("L-1", vence, "10");
        assertThat(lotes.count()).isEqualTo(1);
        assertThat(existencia()).isEqualTo(50);
        assertThat(entradas.count()).isEqualTo(2);
    }

    @Test
    void entradaConOtraFechaParaUnLoteExistenteSeRechaza() throws Exception {
        entrada("L-1", vence, "40");
        entrada("L-1", LocalDate.now().plusYears(2).toString(), "10");
        assertThat(existencia()).isEqualTo(40);
        assertThat(entradas.count()).isEqualTo(1);
    }

    @Test
    void entradaConDatosInvalidosSeRechaza() throws Exception {
        entrada("L-1", vence, "-5");
        entrada("L-1", vence, "0");
        entrada("", vence, "5");
        entrada("L-2", "", "5");
        entrada("L-3", LocalDate.now().minusDays(1).toString(), "5");
        assertThat(existencia()).isZero();
        assertThat(entradas.count()).isZero();
        assertThat(lotes.count()).isZero();
    }

    @Test
    void productoInactivoNoAdmiteNuevasEntradas() throws Exception {
        mvc.perform(post("/productos/{id}/inactivar", paracetamol.getIdProducto())
                .with(user("ana").roles("ADMINISTRADOR")).with(csrf()));
        entrada("L-1", vence, "10");
        assertThat(existencia()).isZero();
    }

    @Test
    void encargadoDeVentasNoPuedeRegistrarEntradasNiSalidas() throws Exception {
        mvc.perform(post("/productos/{id}/entrada", paracetamol.getIdProducto())
                        .with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf())
                        .param("idProveedor", String.valueOf(distribuidora.getIdProveedor()))
                        .param("codigoLote", "L-1").param("fechaVencimiento", vence).param("cantidad", "5"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/productos/{id}/salida", paracetamol.getIdProducto())
                        .with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf())
                        .param("idLote", "1").param("cantidad", "1").param("motivo", "OTRO"))
                .andExpect(status().isForbidden());
        assertThat(existencia()).isZero();
    }

    // ---- Salidas por lote (RF-05)

    @Test
    void salidaDescuentaDelLoteSeleccionadoYGuardaElMotivo() throws Exception {
        entrada("L-1", vence, "40");
        Lote lote = lotes.findByProductoAndCodigoLote(paracetamol, "L-1").orElseThrow();

        mvc.perform(post("/productos/{id}/salida", paracetamol.getIdProducto())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("idLote", String.valueOf(lote.getIdLote()))
                        .param("cantidad", "15").param("motivo", "DANO").param("observacion", "Caja mojada"))
                .andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attributeExists("ok"));

        assertThat(existencia()).isEqualTo(25);
        assertThat(salidas.findAll()).singleElement().satisfies(s -> {
            assertThat(s.getCantidad()).isEqualTo(15);
            assertThat(s.getMotivo().getValorBd()).isEqualTo("dano");
        });
    }

    @Test
    void salidaMayorQueLaExistenciaDelLoteSeRechaza() throws Exception {
        entrada("L-1", vence, "10");
        Lote lote = lotes.findByProductoAndCodigoLote(paracetamol, "L-1").orElseThrow();

        mvc.perform(post("/productos/{id}/salida", paracetamol.getIdProducto())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("idLote", String.valueOf(lote.getIdLote()))
                        .param("cantidad", "11").param("motivo", "VENCIMIENTO"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/salida-form"));

        assertThat(existencia()).isEqualTo(10);
        assertThat(salidas.count()).isZero();
    }

    @Test
    void salidaSinMotivoOConCantidadNoPositivaSeRechaza() throws Exception {
        entrada("L-1", vence, "10");
        Lote lote = lotes.findByProductoAndCodigoLote(paracetamol, "L-1").orElseThrow();

        mvc.perform(post("/productos/{id}/salida", paracetamol.getIdProducto())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("idLote", String.valueOf(lote.getIdLote())).param("cantidad", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/salida-form"));
        assertThat(existencia()).isEqualTo(10);
    }

    @Test
    void salidaDeUnLoteDeOtroProductoSeRechaza() throws Exception {
        Producto otro = productos.save(new Producto("P-009", "Otro", "Caja", "Lab",
                new BigDecimal("100"), 1, 1));
        Lote ajeno = lotes.save(new Lote(otro, "X-1", LocalDate.now().plusYears(1)));
        ajeno.sumar(20);
        lotes.save(ajeno);

        mvc.perform(post("/productos/{id}/salida", paracetamol.getIdProducto())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("idLote", String.valueOf(ajeno.getIdLote()))
                        .param("cantidad", "5").param("motivo", "OTRO"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/salida-form"));
        assertThat(lotes.findById(ajeno.getIdLote()).orElseThrow().getCantidadDisponible()).isEqualTo(20);
    }
}
