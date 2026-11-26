package com.leviplus.ventas.controller;

import com.leviplus.ventas.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Clientes frecuentes (RF-27, RF-28): los dos roles pueden registrarlos y consultarlos. */
@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService servicio;

    public ClienteController(ClienteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String listar(Model modelo) {
        modelo.addAttribute("clientes", servicio.listar());
        return "clientes/clientes";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String nuevo(Model modelo) {
        modelo.addAttribute("clienteForm", new ClienteForm());
        return "clientes/cliente-form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String registrar(@Valid ClienteForm clienteForm, BindingResult errores, RedirectAttributes aviso) {
        if (errores.hasErrors()) {
            return "clientes/cliente-form";
        }
        servicio.registrar(clienteForm);
        aviso.addFlashAttribute("ok", "Cliente frecuente registrado correctamente");
        return "redirect:/clientes";
    }
}
