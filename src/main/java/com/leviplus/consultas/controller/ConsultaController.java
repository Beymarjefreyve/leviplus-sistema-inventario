package com.leviplus.consultas.controller;

import com.leviplus.consultas.service.ConsultaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Alertas (RF-06, RF-07, RF-17) y consulta de existencias (RF-18). */
@Controller
public class ConsultaController {

    private final ConsultaService servicio;

    public ConsultaController(ConsultaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String alertas(Model modelo) {
        modelo.addAttribute("porVencer", servicio.lotesPorVencer());
        modelo.addAttribute("bajoStock", servicio.productosBajoStock());
        return "consultas/alertas";
    }
}
