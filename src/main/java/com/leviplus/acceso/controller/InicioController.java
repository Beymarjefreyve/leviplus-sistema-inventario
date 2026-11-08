package com.leviplus.acceso.controller;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.consultas.service.ConsultaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    private final UsuarioRepository usuarioRepository;
    private final ConsultaService consultas;

    public InicioController(UsuarioRepository usuarioRepository, ConsultaService consultas) {
        this.usuarioRepository = usuarioRepository;
        this.consultas = consultas;
    }

    @GetMapping("/")
    public String inicio(Authentication autenticacion, Model modelo) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(autenticacion.getName()).orElseThrow();
        modelo.addAttribute("nombre", usuario.getNombreCompleto());
        modelo.addAttribute("rol", usuario.getRol().getNombre());
        // RF-15: el panel de indicadores es del administrador.
        modelo.addAttribute("panel", usuario.getRol() == Rol.ADMINISTRADOR ? consultas.panel() : null);
        return "inicio";
    }
}
