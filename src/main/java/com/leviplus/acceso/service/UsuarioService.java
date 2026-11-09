package com.leviplus.acceso.service;

import com.leviplus.acceso.controller.UsuarioForm;
import com.leviplus.acceso.model.EstadoRegistro;
import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.common.ReglaDeNegocioException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private static final int LARGO_MINIMO_CONTRASENA = 8;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findAllByOrderByNombreCompletoAsc();
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Integer id) {
        return usuarios.findById(id).orElseThrow(() -> new ReglaDeNegocioException("El usuario no existe"));
    }

    /** RF-02: registra un usuario activo con el rol indicado. */
    @Transactional
    public Usuario crear(UsuarioForm datos) {
        String nombreUsuario = datos.getNombreUsuario().trim();
        if (usuarios.existsByNombreUsuario(nombreUsuario)) {
            throw new ReglaDeNegocioException("Ya existe un usuario con ese nombre de usuario");
        }
        validarContrasena(datos.getContrasena(), true);
        Usuario nuevo = new Usuario(datos.getNombreCompleto().trim(), nombreUsuario,
                codificador.encode(datos.getContrasena()), datos.getRol());
        return usuarios.save(nuevo);
    }

    /** RF-19: modifica nombre y rol; la contraseña solo cambia si se envía una nueva. */
    @Transactional
    public Usuario actualizar(Integer id, UsuarioForm datos) {
        Usuario usuario = obtener(id);
        if (usuario.esAdministrador() && datos.getRol() != Rol.ADMINISTRADOR) {
            exigirOtroAdministradorActivo(usuario);
        }
        usuario.actualizar(datos.getNombreCompleto().trim(), datos.getRol());
        if (datos.getContrasena() != null && !datos.getContrasena().isBlank()) {
            validarContrasena(datos.getContrasena(), false);
            usuario.cambiarContrasenaHash(codificador.encode(datos.getContrasena()));
        }
        return usuarios.save(usuario);
    }

    /** RF-20: desactiva conservando el registro; no se puede desactivar a sí mismo ni al último administrador. */
    @Transactional
    public void desactivar(Integer id, String nombreUsuarioActual) {
        Usuario usuario = obtener(id);
        if (usuario.getNombreUsuario().equals(nombreUsuarioActual)) {
            throw new ReglaDeNegocioException("No puedes desactivar tu propio usuario");
        }
        if (!usuario.estaActivo()) {
            throw new ReglaDeNegocioException("El usuario ya está inactivo");
        }
        if (usuario.esAdministrador()) {
            exigirOtroAdministradorActivo(usuario);
        }
        usuario.desactivar();
        usuarios.save(usuario);
    }

    /** Vuelve a permitir el ingreso de un usuario desactivado; conserva su registro y su rol. */
    @Transactional
    public void activar(Integer id) {
        Usuario usuario = obtener(id);
        if (usuario.estaActivo()) {
            throw new ReglaDeNegocioException("El usuario ya está activo");
        }
        usuario.activar();
        usuarios.save(usuario);
    }

    private void exigirOtroAdministradorActivo(Usuario usuario) {
        if (usuario.estaActivo()
                && usuarios.countByRolAndEstado(Rol.ADMINISTRADOR, EstadoRegistro.ACTIVO) <= 1) {
            throw new ReglaDeNegocioException("Debe quedar al menos un administrador activo");
        }
    }

    private void validarContrasena(String contrasena, boolean obligatoria) {
        boolean vacia = contrasena == null || contrasena.isBlank();
        if (vacia && obligatoria) {
            throw new ReglaDeNegocioException("La contraseña es obligatoria");
        }
        if (!vacia && contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            throw new ReglaDeNegocioException("La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres");
        }
    }
}
