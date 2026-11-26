package com.leviplus.consultas.controller;

import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.service.ProductoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Pantalla de acceso rápido: se escribe o escanea el código de un producto y se ofrecen las opciones que el
 * sistema ya tiene (consultar existencia y lotes, registrar entrada o salida, editar). No agrega funciones
 * nuevas; solo enlaza las existentes.
 */
@Controller
public class EscanerController {

    private final ProductoService productos;

    public EscanerController(ProductoService productos) {
        this.productos = productos;
    }

    @GetMapping("/escaner")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String escaner(@RequestParam(required = false) String codigo, Model modelo) {
        String texto = codigo == null ? "" : codigo.trim();
        List<Producto> encontrados = productos.buscarPorCodigo(texto);
        Map<Integer, Long> existencias = productos.existencias();
        modelo.addAttribute("codigo", texto);
        modelo.addAttribute("buscado", !texto.isEmpty());
        modelo.addAttribute("encontrados", encontrados);
        modelo.addAttribute("existencias", existencias);
        // Un solo resultado: se abre directo su menú de opciones.
        modelo.addAttribute("producto", encontrados.size() == 1 ? encontrados.get(0) : null);
        return "consultas/escaner";
    }
}
