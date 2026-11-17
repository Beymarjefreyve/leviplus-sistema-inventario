package com.leviplus.inventario.controller;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.model.MotivoSalida;
import com.leviplus.inventario.service.InventarioService;
import com.leviplus.inventario.service.ProductoService;
import com.leviplus.inventario.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Entradas y salidas de inventario por lote (RF-04 y RF-05). */
@Controller
@RequestMapping("/productos/{id}")
public class InventarioController {

    private final InventarioService inventario;
    private final ProductoService productos;
    private final ProveedorService proveedores;

    public InventarioController(InventarioService inventario, ProductoService productos,
                                ProveedorService proveedores) {
        this.inventario = inventario;
        this.productos = productos;
        this.proveedores = proveedores;
    }

    @GetMapping("/entrada")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String formularioEntrada(@PathVariable Integer id, Model modelo) {
        modelo.addAttribute("entradaForm", new EntradaForm());
        prepararEntrada(id, modelo);
        return "inventario/entrada-form";
    }

    @PostMapping("/entrada")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String registrarEntrada(@PathVariable Integer id, @Valid EntradaForm entradaForm, BindingResult errores,
                                   Authentication autenticacion, Model modelo, RedirectAttributes aviso) {
        if (!errores.hasErrors()) {
            try {
                inventario.registrarEntrada(id, entradaForm, autenticacion.getName());
                aviso.addFlashAttribute("ok", "Entrada registrada; la existencia del producto se actualizó");
                return "redirect:/inventario";
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        prepararEntrada(id, modelo);
        return "inventario/entrada-form";
    }

    @GetMapping("/salida")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String formularioSalida(@PathVariable Integer id, Model modelo) {
        modelo.addAttribute("salidaForm", new SalidaForm());
        prepararSalida(id, modelo);
        return "inventario/salida-form";
    }

    @PostMapping("/salida")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String registrarSalida(@PathVariable Integer id, @Valid SalidaForm salidaForm, BindingResult errores,
                                  Authentication autenticacion, Model modelo, RedirectAttributes aviso) {
        if (!errores.hasErrors()) {
            try {
                inventario.registrarSalida(id, salidaForm, autenticacion.getName());
                aviso.addFlashAttribute("ok", "Salida registrada; la existencia del lote se actualizó");
                return "redirect:/inventario";
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        prepararSalida(id, modelo);
        return "inventario/salida-form";
    }

    private void prepararEntrada(Integer id, Model modelo) {
        modelo.addAttribute("producto", productos.obtener(id));
        modelo.addAttribute("existencia", inventario.existenciaDe(id));
        modelo.addAttribute("proveedores", proveedores.listarActivos());
        modelo.addAttribute("lotes", inventario.lotesDe(id));
    }

    private void prepararSalida(Integer id, Model modelo) {
        modelo.addAttribute("producto", productos.obtener(id));
        modelo.addAttribute("existencia", inventario.existenciaDe(id));
        modelo.addAttribute("lotes", inventario.lotesDisponibles(id));
        modelo.addAttribute("motivos", MotivoSalida.values());
    }
}
