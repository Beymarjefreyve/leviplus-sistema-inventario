package com.leviplus.inventario.controller;

import com.leviplus.inventario.service.ProductoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Pantalla de entradas y salidas: un buscador de productos desde el cual se abre el formulario de entrada o de
 * salida (RF-04 y RF-05). Los formularios siguen siendo los de {@link InventarioController}.
 */
@Controller
@RequestMapping("/inventario")
public class MovimientosController {

    private static final int LIMITE_FILAS = 20;

    private final ProductoService productos;

    public MovimientosController(ProductoService productos) {
        this.productos = productos;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String buscar(@RequestParam(required = false) String q, Model modelo) {
        modelo.addAttribute("q", q == null ? "" : q);
        modelo.addAttribute("limite", LIMITE_FILAS);
        modelo.addAttribute("productos", productos.buscar(q, "activos", LIMITE_FILAS));
        modelo.addAttribute("existencias", productos.existencias());
        return "inventario/movimientos";
    }
}
