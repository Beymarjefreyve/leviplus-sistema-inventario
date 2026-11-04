package com.leviplus.acceso.controller;

import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    private final UsuarioRepository usuarioRepository;

    public InicioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/")
    public String inicio(Authentication autenticacion, Model modelo) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(autenticacion.getName()).orElseThrow();
        modelo.addAttribute("nombre", usuario.getNombreCompleto());
        modelo.addAttribute("rol", usuario.getRol().getNombre());
        return "inicio";
    }
}
