package com.leviplus.finanzas.controller;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.finanzas.service.MovimientoService;
import com.leviplus.inventario.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** Registro de egresos (RF-12) y otros ingresos (RF-13). */
@Controller
@RequestMapping("/finanzas")
public class FinanzasController {

    private final MovimientoService servicio;
    private final ProveedorService proveedores;

    public FinanzasController(MovimientoService servicio, ProveedorService proveedores) {
        this.servicio = servicio;
        this.proveedores = proveedores;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String listar(Model modelo) {
        modelo.addAttribute("movimientos", servicio.listar());
        return "finanzas/movimientos";
    }

    @GetMapping("/egresos/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String nuevoEgreso(Model modelo) {
        prepararFormulario(modelo, true, nuevoFormulario());
        return "finanzas/movimiento-form";
    }

    @PostMapping("/egresos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String registrarEgreso(@Valid MovimientoForm movimientoForm, BindingResult errores,
                                  Authentication autenticacion, Model modelo, RedirectAttributes aviso) {
        if (!errores.hasErrors()) {
            try {
                servicio.registrarEgreso(movimientoForm, autenticacion.getName());
                aviso.addFlashAttribute("ok", "Egreso registrado correctamente");
                return "redirect:/finanzas";
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        prepararFormulario(modelo, true, null);
        return "finanzas/movimiento-form";
    }

    @GetMapping("/ingresos/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String nuevoIngreso(Model modelo) {
        prepararFormulario(modelo, false, nuevoFormulario());
        return "finanzas/movimiento-form";
    }

    @PostMapping("/ingresos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String registrarIngreso(@Valid MovimientoForm movimientoForm, BindingResult errores,
                                   Authentication autenticacion, Model modelo, RedirectAttributes aviso) {
        if (!errores.hasErrors()) {
            try {
                servicio.registrarIngreso(movimientoForm, autenticacion.getName());
                aviso.addFlashAttribute("ok", "Ingreso registrado correctamente");
                return "redirect:/finanzas";
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        prepararFormulario(modelo, false, null);
        return "finanzas/movimiento-form";
    }

    private MovimientoForm nuevoFormulario() {
        MovimientoForm form = new MovimientoForm();
        form.setFecha(LocalDate.now());
        return form;
    }

    private void prepararFormulario(Model modelo, boolean esEgreso, MovimientoForm inicial) {
        if (inicial != null) {
            modelo.addAttribute("movimientoForm", inicial);
        }
        modelo.addAttribute("esEgreso", esEgreso);
        modelo.addAttribute("hoy", LocalDate.now());
        modelo.addAttribute("proveedores", esEgreso ? proveedores.listarActivos() : java.util.List.of());
    }
}
