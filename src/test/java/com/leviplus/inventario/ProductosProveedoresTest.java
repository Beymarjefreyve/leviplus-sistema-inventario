package com.leviplus.inventario;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.model.Proveedor;
import com.leviplus.inventario.repository.LoteRepository;
import com.leviplus.inventario.repository.ProductoRepository;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductosProveedoresTest {

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
    PasswordEncoder codificador;

    Producto paracetamol;

    @BeforeEach
    void preparar() {
        limpiar();
        usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        usuarios.save(new Usuario("Beto Ventas", "beto", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS));
        paracetamol = productos.save(new Producto("P-001", "Paracetamol 500 mg", "Caja x 10", "Genfar",
                new BigDecimal("3500.00"), 5, 30));
        proveedores.save(new Proveedor("Distribuidora Cúcuta", "3001234567", null));
    }

    @AfterEach
    void limpiar() {
        lotes.deleteAll();
        productos.deleteAll();
        proveedores.deleteAll();
        usuarios.deleteAll();
    }

    // ---- Productos (RF-03, RF-23, RF-24)

    @Test
    void administradorRegistraProductoSinExistencias() throws Exception {
        mvc.perform(post("/productos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("codigoProducto", "P-002").param("nombre", "Ibuprofeno 400 mg")
                        .param("presentacion", "Caja x 20").param("laboratorio", "MK")
                        .param("precioVenta", "5200").param("stockMinimo", "10").param("diasAlertaVencimiento", "45"))
                .andExpect(redirectedUrl("/productos"));
        assertThat(productos.existsByCodigoProducto("P-002")).isTrue();
        assertThat(lotes.existenciaPorProducto()).isEmpty();
    }

    @Test
    void codigoDeProductoRepetidoSeRechaza() throws Exception {
        mvc.perform(post("/productos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("codigoProducto", "P-001").param("nombre", "Otro")
                        .param("presentacion", "Caja").param("laboratorio", "X")
                        .param("precioVenta", "100").param("stockMinimo", "1").param("diasAlertaVencimiento", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/producto-form"));
        assertThat(productos.count()).isEqualTo(1);
    }

    @Test
    void productoConCamposVaciosONegativosNoSeGuarda() throws Exception {
        mvc.perform(post("/productos").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("codigoProducto", "").param("nombre", "")
                        .param("precioVenta", "-5").param("stockMinimo", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/producto-form"));
        assertThat(productos.count()).isEqualTo(1);
    }

    @Test
    void editarProductoActualizaSusDatos() throws Exception {
        mvc.perform(post("/productos/{id}", paracetamol.getIdProducto())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("codigoProducto", "P-001").param("nombre", "Paracetamol 650 mg")
                        .param("presentacion", "Caja x 10").param("laboratorio", "Genfar")
                        .param("precioVenta", "4000").param("stockMinimo", "8").param("diasAlertaVencimiento", "60"))
                .andExpect(redirectedUrl("/productos"));
        Producto editado = productos.findById(paracetamol.getIdProducto()).orElseThrow();
        assertThat(editado.getNombre()).isEqualTo("Paracetamol 650 mg");
        assertThat(editado.getPrecioVenta()).isEqualByComparingTo("4000");
        assertThat(editado.getStockMinimo()).isEqualTo(8);
    }

    @Test
    void inactivarProductoConservaElRegistro() throws Exception {
        mvc.perform(post("/productos/{id}/inactivar", paracetamol.getIdProducto())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(redirectedUrl("/productos"));
        assertThat(productos.findById(paracetamol.getIdProducto()).orElseThrow().estaActivo()).isFalse();
    }

    @Test
    void encargadoDeVentasNoAccedeAProductosNiProveedores() throws Exception {
        mvc.perform(get("/productos").with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/proveedores").with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/productos/{id}/inactivar", paracetamol.getIdProducto())
                        .with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(productos.findById(paracetamol.getIdProducto()).orElseThrow().estaActivo()).isTrue();
    }

    @Test
    void reactivarProductoLoDejaDisponibleOtraVez() throws Exception {
        paracetamol.inactivar();
        productos.save(paracetamol);
        mvc.perform(post("/productos/{id}/reactivar", paracetamol.getIdProducto())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attributeExists("ok"));
        assertThat(productos.findById(paracetamol.getIdProducto()).orElseThrow().estaActivo()).isTrue();
        mvc.perform(post("/productos/{id}/reactivar", paracetamol.getIdProducto())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(flash().attributeExists("error"));
        mvc.perform(post("/productos/{id}/reactivar", paracetamol.getIdProducto())
                        .with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listadoDeProductosSeBuscaPorTextoYEstado() throws Exception {
        productos.save(new Producto("P-002", "Ibuprofeno 400 mg", "Caja x 10", "MK", new BigDecimal("8900.00"), 5, 30));
        mvc.perform(get("/productos").param("q", "ibupro").with(user("ana").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Ibuprofeno 400 mg")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Paracetamol 500 mg"))));
        paracetamol.inactivar();
        productos.save(paracetamol);
        mvc.perform(get("/productos").param("estado", "inactivos").with(user("ana").roles("ADMINISTRADOR")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Paracetamol 500 mg")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Ibuprofeno 400 mg"))));
    }

    @Test
    void pantallaDeEntradasYSalidasBuscaProductosActivos() throws Exception {
        productos.save(new Producto("P-002", "Ibuprofeno 400 mg", "Caja x 10", "MK", new BigDecimal("8900.00"), 5, 30));
        mvc.perform(get("/inventario").param("q", "paracet").with(user("ana").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/movimientos"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Paracetamol 500 mg")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Ibuprofeno 400 mg"))));
        mvc.perform(get("/inventario").with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(status().isForbidden());
    }

    // ---- Proveedores (RF-21, RF-22)

    @Test
    void reactivarProveedorLoDejaDisponibleOtraVez() throws Exception {
        Proveedor proveedor = proveedores.findAll().get(0);
        mvc.perform(post("/proveedores/{id}/inactivar", proveedor.getIdProveedor())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(redirectedUrl("/proveedores"));
        assertThat(proveedores.findById(proveedor.getIdProveedor()).orElseThrow().estaActivo()).isFalse();
        mvc.perform(post("/proveedores/{id}/reactivar", proveedor.getIdProveedor())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(redirectedUrl("/proveedores"))
                .andExpect(flash().attributeExists("ok"));
        assertThat(proveedores.findById(proveedor.getIdProveedor()).orElseThrow().estaActivo()).isTrue();
    }

    @Test
    void administradorRegistraYListaProveedores() throws Exception {
        mvc.perform(post("/proveedores").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombre", "Droguería Mayorista").param("telefono", "3109998877")
                        .param("correo", "ventas@mayorista.co"))
                .andExpect(redirectedUrl("/proveedores"));
        assertThat(proveedores.findAll()).hasSize(2);
        mvc.perform(get("/proveedores").with(user("ana").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/proveedores"));
    }

    @Test
    void proveedorSinNombreOTelefonoNoSeGuarda() throws Exception {
        mvc.perform(post("/proveedores").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombre", "").param("telefono", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("inventario/proveedor-form"));
        assertThat(proveedores.count()).isEqualTo(1);
    }
}
