package com.leviplus.ventas;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.ventas.repository.ClienteFrecuenteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientesPedidosTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ClienteFrecuenteRepository clientes;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder codificador;

    @BeforeEach
    void preparar() {
        limpiar();
        usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        usuarios.save(new Usuario("Beto Ventas", "beto", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS));
    }

    @AfterEach
    void limpiar() {
        clientes.deleteAll();
        usuarios.deleteAll();
    }

    // ---- Clientes frecuentes (RF-27, RF-28)

    @Test
    void ambosRolesRegistranYConsultanClientes() throws Exception {
        for (String rol : new String[]{"ADMINISTRADOR", "ENCARGADO_DE_VENTAS"}) {
            mvc.perform(post("/clientes").with(user("x").roles(rol)).with(csrf())
                            .param("nombre", "Cliente " + rol).param("telefono", "3001234567"))
                    .andExpect(redirectedUrl("/clientes"));
        }
        assertThat(clientes.count()).isEqualTo(2);
        mvc.perform(get("/clientes").with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cliente ADMINISTRADOR")))
                .andExpect(content().string(containsString("3001234567")));
    }

    @Test
    void clienteExigeNombreYTelefonoValido() throws Exception {
        mvc.perform(post("/clientes").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombre", " ").param("telefono", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes/cliente-form"))
                .andExpect(model().attributeHasFieldErrors("clienteForm", "nombre", "telefono"));
        assertThat(clientes.count()).isZero();
    }

    @Test
    void formulariosDeClientesAbrenParaAmbosRolesYExigenSesion() throws Exception {
        for (String ruta : new String[]{"/clientes", "/clientes/nuevo"}) {
            for (String rol : new String[]{"ADMINISTRADOR", "ENCARGADO_DE_VENTAS"}) {
                mvc.perform(get(ruta).with(user("x").roles(rol))).andExpect(status().isOk());
            }
            mvc.perform(get(ruta)).andExpect(status().is3xxRedirection());
        }
    }
}
