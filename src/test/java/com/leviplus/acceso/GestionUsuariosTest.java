package com.leviplus.acceso;

import com.leviplus.acceso.model.EstadoRegistro;
import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GestionUsuariosTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder codificador;

    Usuario ana;
    Usuario beto;

    @BeforeEach
    void preparar() {
        usuarios.deleteAll();
        ana = usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        beto = usuarios.save(new Usuario("Beto Ventas", "beto", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS));
    }

    @Test
    void administradorRegistraUsuarioActivoConContrasenaCifrada() throws Exception {
        mvc.perform(post("/usuarios").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombreCompleto", "Carla Caja")
                        .param("nombreUsuario", "carla")
                        .param("contrasena", "otra-clave-segura")
                        .param("rol", "ENCARGADO_DE_VENTAS"))
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attributeExists("ok"));

        Usuario creado = usuarios.findByNombreUsuario("carla").orElseThrow();
        assertThat(creado.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(creado.getRol()).isEqualTo(Rol.ENCARGADO_DE_VENTAS);
        assertThat(creado.getContrasenaHash()).isNotEqualTo("otra-clave-segura");
        assertThat(codificador.matches("otra-clave-segura", creado.getContrasenaHash())).isTrue();
    }

    @Test
    void encargadoDeVentasNoPuedeEntrarAlModuloDeUsuarios() throws Exception {
        mvc.perform(get("/usuarios").with(user("beto").roles("ENCARGADO_DE_VENTAS")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios").with(user("beto").roles("ENCARGADO_DE_VENTAS")).with(csrf())
                        .param("nombreCompleto", "X").param("nombreUsuario", "xxx")
                        .param("contrasena", "12345678").param("rol", "ADMINISTRADOR"))
                .andExpect(status().isForbidden());
        assertThat(usuarios.existsByNombreUsuario("xxx")).isFalse();
    }

    @Test
    void datosIncompletosNoGuardanElUsuario() throws Exception {
        mvc.perform(post("/usuarios").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombreCompleto", "")
                        .param("nombreUsuario", "")
                        .param("contrasena", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("acceso/usuario-form"));
        assertThat(usuarios.count()).isEqualTo(2);
    }

    @Test
    void nombreDeUsuarioRepetidoSeRechaza() throws Exception {
        mvc.perform(post("/usuarios").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombreCompleto", "Otro Beto")
                        .param("nombreUsuario", "beto")
                        .param("contrasena", "clave-larga-123")
                        .param("rol", "ENCARGADO_DE_VENTAS"))
                .andExpect(status().isOk())
                .andExpect(view().name("acceso/usuario-form"));
        assertThat(usuarios.count()).isEqualTo(2);
    }

    @Test
    void contrasenaCortaSeRechaza() throws Exception {
        mvc.perform(post("/usuarios").with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombreCompleto", "Carla Caja")
                        .param("nombreUsuario", "carla")
                        .param("contrasena", "corta")
                        .param("rol", "ENCARGADO_DE_VENTAS"))
                .andExpect(status().isOk())
                .andExpect(view().name("acceso/usuario-form"));
        assertThat(usuarios.existsByNombreUsuario("carla")).isFalse();
    }

    @Test
    void editarCambiaNombreYRolSinTocarLaContrasena() throws Exception {
        String hashAntes = beto.getContrasenaHash();
        mvc.perform(post("/usuarios/{id}", beto.getIdUsuario()).with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombreCompleto", "Beto Administrador")
                        .param("nombreUsuario", "beto")
                        .param("contrasena", "")
                        .param("rol", "ADMINISTRADOR"))
                .andExpect(redirectedUrl("/usuarios"));

        Usuario editado = usuarios.findById(beto.getIdUsuario()).orElseThrow();
        assertThat(editado.getNombreCompleto()).isEqualTo("Beto Administrador");
        assertThat(editado.getRol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(editado.getContrasenaHash()).isEqualTo(hashAntes);
    }

    @Test
    void desactivarConservaElRegistroYBloqueaElAcceso() throws Exception {
        mvc.perform(post("/usuarios/{id}/desactivar", beto.getIdUsuario())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attributeExists("ok"));

        Usuario desactivado = usuarios.findById(beto.getIdUsuario()).orElseThrow();
        assertThat(desactivado.getEstado()).isEqualTo(EstadoRegistro.INACTIVO);

        mvc.perform(post("/login").param("username", "beto").param("password", "clave-de-prueba").with(csrf()))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void nadiePuedeDesactivarseASiMismo() throws Exception {
        mvc.perform(post("/usuarios/{id}/desactivar", ana.getIdUsuario())
                        .with(user("ana").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(flash().attributeExists("error"));
        assertThat(usuarios.findById(ana.getIdUsuario()).orElseThrow().estaActivo()).isTrue();
    }

    @Test
    void noSePuedeDejarElSistemaSinAdministradorActivo() throws Exception {
        mvc.perform(post("/usuarios/{id}", ana.getIdUsuario()).with(user("ana").roles("ADMINISTRADOR")).with(csrf())
                        .param("nombreCompleto", "Ana Admin")
                        .param("nombreUsuario", "ana")
                        .param("rol", "ENCARGADO_DE_VENTAS"))
                .andExpect(status().isOk())
                .andExpect(view().name("acceso/usuario-form"));
        assertThat(usuarios.findById(ana.getIdUsuario()).orElseThrow().getRol()).isEqualTo(Rol.ADMINISTRADOR);
    }
}
