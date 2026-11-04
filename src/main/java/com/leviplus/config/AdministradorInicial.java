package com.leviplus.config;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder codificador;
    private final String usuario;
    private final String contrasena;
    private final String nombre;

    public AdministradorInicial(UsuarioRepository usuarioRepository,
                                PasswordEncoder codificador,
                                @Value("${leviplus.admin.usuario:}") String usuario,
                                @Value("${leviplus.admin.contrasena:}") String contrasena,
                                @Value("${leviplus.admin.nombre:Administrador}") String nombre) {
        this.usuarioRepository = usuarioRepository;
        this.codificador = codificador;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.nombre = nombre;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (usuario.isBlank() || contrasena.isBlank()) {
            log.warn("No hay usuarios y no se definieron LEVIPLUS_ADMIN_USER / LEVIPLUS_ADMIN_PASSWORD: "
                    + "no se puede iniciar sesion hasta crear un administrador.");
            return;
        }
        usuarioRepository.save(new Usuario(nombre, usuario, codificador.encode(contrasena), Rol.ADMINISTRADOR));
        log.info("Administrador inicial '{}' creado.", usuario);
    }
}
