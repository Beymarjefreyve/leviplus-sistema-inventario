package com.leviplus.inventario.controller;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.model.Proveedor;
import com.leviplus.inventario.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorService servicio;

    public ProveedorController(ProveedorService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String listar(Model modelo) {
        modelo.addAttribute("proveedores", servicio.listar());
        return "inventario/proveedores";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String nuevo(Model modelo) {
        modelo.addAttribute("proveedorForm", new ProveedorForm());
        modelo.addAttribute("proveedorEditado", null);
        return "inventario/proveedor-form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String crear(@Valid ProveedorForm proveedorForm, BindingResult errores, Model modelo,
                        RedirectAttributes aviso) {
        if (errores.hasErrors()) {
            modelo.addAttribute("proveedorEditado", null);
            return "inventario/proveedor-form";
        }
        servicio.crear(proveedorForm);
        aviso.addFlashAttribute("ok", "Proveedor registrado correctamente");
        return "redirect:/proveedores";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String editar(@PathVariable Integer id, Model modelo) {
        Proveedor proveedor = servicio.obtener(id);
        ProveedorForm form = new ProveedorForm();
        form.setNombre(proveedor.getNombre());
        form.setTelefono(proveedor.getTelefono());
        form.setCorreo(proveedor.getCorreo());
        modelo.addAttribute("proveedorForm", form);
        modelo.addAttribute("proveedorEditado", proveedor);
        return "inventario/proveedor-form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String actualizar(@PathVariable Integer id, @Valid ProveedorForm proveedorForm, BindingResult errores,
                             Model modelo, RedirectAttributes aviso) {
        if (errores.hasErrors()) {
            modelo.addAttribute("proveedorEditado", servicio.obtener(id));
            return "inventario/proveedor-form";
        }
        servicio.actualizar(id, proveedorForm);
        aviso.addFlashAttribute("ok", "Proveedor actualizado correctamente");
        return "redirect:/proveedores";
    }

    @PostMapping("/{id}/inactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String inactivar(@PathVariable Integer id, RedirectAttributes aviso) {
        try {
            servicio.inactivar(id);
            aviso.addFlashAttribute("ok", "Proveedor inactivado");
        } catch (ReglaDeNegocioException e) {
            aviso.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/proveedores";
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String reactivar(@PathVariable Integer id, RedirectAttributes aviso) {
        try {
            servicio.reactivar(id);
            aviso.addFlashAttribute("ok", "Proveedor reactivado");
        } catch (ReglaDeNegocioException e) {
            aviso.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/proveedores";
    }
}
