package com.leviplus.inventario.controller;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private static final int LIMITE_FILAS = 100;

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false, defaultValue = "todos") String estado, Model modelo) {
        modelo.addAttribute("productos", servicio.buscar(q, estado, LIMITE_FILAS));
        modelo.addAttribute("total", servicio.listar().size());
        modelo.addAttribute("limite", LIMITE_FILAS);
        modelo.addAttribute("q", q == null ? "" : q);
        modelo.addAttribute("estado", estado);
        modelo.addAttribute("existencias", servicio.existencias());
        return "inventario/productos";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String nuevo(Model modelo) {
        modelo.addAttribute("productoForm", new ProductoForm());
        modelo.addAttribute("productoEditado", null);
        return "inventario/producto-form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String crear(@Valid ProductoForm productoForm, BindingResult errores, Model modelo,
                        RedirectAttributes aviso) {
        if (!errores.hasErrors()) {
            try {
                servicio.crear(productoForm);
                aviso.addFlashAttribute("ok", "Producto registrado correctamente");
                return "redirect:/productos";
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        modelo.addAttribute("productoEditado", null);
        return "inventario/producto-form";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String editar(@PathVariable Integer id, Model modelo) {
        Producto producto = servicio.obtener(id);
        ProductoForm form = new ProductoForm();
        form.setCodigoProducto(producto.getCodigoProducto());
        form.setNombre(producto.getNombre());
        form.setPresentacion(producto.getPresentacion());
        form.setLaboratorio(producto.getLaboratorio());
        form.setPrecioVenta(producto.getPrecioVenta());
        form.setStockMinimo(producto.getStockMinimo());
        form.setDiasAlertaVencimiento(producto.getDiasAlertaVencimiento());
        modelo.addAttribute("productoForm", form);
        modelo.addAttribute("productoEditado", producto);
        return "inventario/producto-form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String actualizar(@PathVariable Integer id, @Valid ProductoForm productoForm, BindingResult errores,
                             Model modelo, RedirectAttributes aviso) {
        if (!errores.hasErrors()) {
            try {
                servicio.actualizar(id, productoForm);
                aviso.addFlashAttribute("ok", "Producto actualizado correctamente");
                return "redirect:/productos";
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        modelo.addAttribute("productoEditado", servicio.obtener(id));
        return "inventario/producto-form";
    }

    @PostMapping("/{id}/inactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String inactivar(@PathVariable Integer id, RedirectAttributes aviso) {
        try {
            servicio.inactivar(id);
            aviso.addFlashAttribute("ok", "Producto inactivado; su historial se conserva");
        } catch (ReglaDeNegocioException e) {
            aviso.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/productos";
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String reactivar(@PathVariable Integer id, RedirectAttributes aviso) {
        try {
            servicio.reactivar(id);
            aviso.addFlashAttribute("ok", "Producto reactivado; ya está disponible en entradas y ventas");
        } catch (ReglaDeNegocioException e) {
            aviso.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/productos";
    }
}
