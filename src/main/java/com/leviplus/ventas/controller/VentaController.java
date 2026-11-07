package com.leviplus.ventas.controller;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.ventas.model.Venta;
import com.leviplus.ventas.service.VentaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Registro de ventas (RF-08, RF-09). */
@Controller
@RequestMapping("/ventas")
public class VentaController {

    private static final int FILAS_INICIALES = 3;

    private final VentaService servicio;

    public VentaController(VentaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/nueva")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String nueva(Model modelo) {
        VentaForm form = new VentaForm();
        for (int i = 0; i < FILAS_INICIALES; i++) {
            form.getLineas().add(new VentaForm.Linea());
        }
        modelo.addAttribute("ventaForm", form);
        modelo.addAttribute("productos", servicio.productosVendibles());
        return "ventas/venta-form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String registrar(@Valid VentaForm ventaForm, BindingResult errores, Authentication autenticacion,
                            Model modelo) {
        if (!errores.hasErrors()) {
            try {
                Venta venta = servicio.registrar(ventaForm, autenticacion.getName());
                return "redirect:/ventas/" + venta.getIdVenta();
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        while (ventaForm.getLineas().size() < FILAS_INICIALES) {
            ventaForm.getLineas().add(new VentaForm.Linea());
        }
        modelo.addAttribute("productos", servicio.productosVendibles());
        return "ventas/venta-form";
    }

    /** Comprobante de la venta. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String comprobante(@PathVariable Integer id, Model modelo) {
        modelo.addAttribute("venta", servicio.obtener(id));
        return "ventas/comprobante";
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String anular(@PathVariable Integer id, @RequestParam String motivo, Authentication autenticacion,
                         RedirectAttributes aviso) {
        try {
            servicio.anular(id, motivo, autenticacion.getName());
            aviso.addFlashAttribute("ok", "Venta anulada; los productos volvieron al inventario");
        } catch (ReglaDeNegocioException e) {
            aviso.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventas/" + id;
    }
}
