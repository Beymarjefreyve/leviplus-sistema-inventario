package com.leviplus.acceso;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-01: inicio de sesion, rechazo de credenciales invalidas y de cuentas desactivadas. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccesoTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder codificador;

    @BeforeEach
    void preparar() {
        usuarios.deleteAll();
        usuarios.save(new Usuario("Ana Admin", "ana", codificador.encode("clave-de-prueba"), Rol.ADMINISTRADOR));
        Usuario inactivo = new Usuario("Inés Inactiva", "ines", codificador.encode("clave-de-prueba"), Rol.ENCARGADO_DE_VENTAS);
        inactivo.desactivar();
        usuarios.save(inactivo);
    }

    @Test
    void sinSesionRedirigeAlAcceso() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void pantallaDeAccesoEsPublica() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void credencialesCorrectasInicianSesion() throws Exception {
        mvc.perform(post("/login").param("username", "ana").param("password", "clave-de-prueba").with(csrf()))
                .andExpect(authenticated().withRoles("ADMINISTRADOR"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void contrasenaIncorrectaSeRechaza() throws Exception {
        mvc.perform(post("/login").param("username", "ana").param("password", "otra").with(csrf()))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void usuarioInactivoNoPuedeIngresar() throws Exception {
        mvc.perform(post("/login").param("username", "ines").param("password", "clave-de-prueba").with(csrf()))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void loginSinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/login").param("username", "ana").param("password", "clave-de-prueba"))
                .andExpect(status().isForbidden());
    }
}
