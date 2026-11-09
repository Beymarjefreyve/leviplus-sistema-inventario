package com.leviplus.ventas.controller;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.ventas.model.DestinoDevolucion;
import com.leviplus.ventas.model.DetalleVenta;
import com.leviplus.ventas.model.Venta;
import com.leviplus.ventas.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Ventas (RF-08, RF-09), historial (RF-10, RF-11) y anulación (RF-25). */
@Controller
@RequestMapping("/ventas")
public class VentaController {

    private static final int RESULTADOS_BUSQUEDA = 20;

    /** Línea del carrito con los datos que necesita la pantalla para dibujarla. */
    public record LineaCarrito(Integer idProducto, String nombre, String presentacion, String codigo,
                               BigDecimal precio, long existencia, int cantidad) {
    }

    private final VentaService servicio;

    public VentaController(VentaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/nueva")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String nueva(Model modelo) {
        modelo.addAttribute("ventaForm", new VentaForm());
        modelo.addAttribute("carrito", List.of());
        return "ventas/venta-form";
    }

    /** Buscador del formulario de venta (JSON): nombre, presentación o código; máximo 20 resultados. */
    @GetMapping("/productos")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public List<VentaService.ProductoVendible> buscarProductos(@RequestParam(required = false) String q) {
        return servicio.buscarVendibles(q, RESULTADOS_BUSQUEDA);
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
        modelo.addAttribute("carrito", carritoDe(ventaForm));
        return "ventas/venta-form";
    }

    /** Reconstruye el carrito con las líneas que el usuario había enviado, para no perderlas tras un error. */
    private List<LineaCarrito> carritoDe(VentaForm form) {
        List<Integer> ids = new ArrayList<>();
        for (VentaForm.Linea linea : form.getLineas()) {
            if (linea != null && linea.getIdProducto() != null) {
                ids.add(linea.getIdProducto());
            }
        }
        Map<Integer, VentaService.ProductoVendible> datos = servicio.vendiblesPorIds(ids);
        List<LineaCarrito> carrito = new ArrayList<>();
        for (VentaForm.Linea linea : form.getLineas()) {
            if (linea == null || linea.getIdProducto() == null || !datos.containsKey(linea.getIdProducto())) {
                continue;
            }
            VentaService.ProductoVendible p = datos.get(linea.getIdProducto());
            carrito.add(new LineaCarrito(p.idProducto(), p.nombre(), p.presentacion(), p.codigo(), p.precio(),
                    p.existencia(), linea.getCantidad() == null ? 1 : linea.getCantidad()));
        }
        return carrito;
    }

    /** Comprobante de la venta. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String comprobante(@PathVariable Integer id, Model modelo) {
        modelo.addAttribute("venta", servicio.obtener(id));
        List<com.leviplus.ventas.model.Devolucion> devoluciones = servicio.devolucionesDe(id);
        modelo.addAttribute("devoluciones", devoluciones);
        modelo.addAttribute("totalDevuelto", devoluciones.stream()
                .map(com.leviplus.ventas.model.Devolucion::getValorDevuelto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return "ventas/comprobante";
    }

    /** RF-26: formulario de devolución de productos de una venta registrada. */
    @GetMapping("/{id}/devolucion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String nuevaDevolucion(@PathVariable Integer id, Model modelo) {
        Venta venta = servicio.obtener(id);
        DevolucionForm form = new DevolucionForm();
        for (DetalleVenta detalle : venta.getDetalles()) {
            DevolucionForm.Linea linea = new DevolucionForm.Linea();
            linea.setIdDetalleVenta(detalle.getIdDetalleVenta());
            form.getLineas().add(linea);
        }
        prepararDevolucion(modelo, venta);
        modelo.addAttribute("devolucionForm", form);
        return "ventas/devolucion-form";
    }

    @PostMapping("/{id}/devoluciones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String registrarDevolucion(@PathVariable Integer id, @Valid DevolucionForm devolucionForm,
                                      BindingResult errores, Authentication autenticacion, Model modelo,
                                      RedirectAttributes aviso) {
        if (!errores.hasErrors()) {
            try {
                servicio.registrarDevolucion(id, devolucionForm, autenticacion.getName());
                aviso.addFlashAttribute("ok", "Devolución registrada correctamente");
                return "redirect:/ventas/" + id;
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        prepararDevolucion(modelo, servicio.obtener(id));
        return "ventas/devolucion-form";
    }

    private void prepararDevolucion(Model modelo, Venta venta) {
        modelo.addAttribute("venta", venta);
        modelo.addAttribute("devolvibles", servicio.devolvibles(venta));
        modelo.addAttribute("destinos", DestinoDevolucion.values());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String historial(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            Model modelo) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(30);
        modelo.addAttribute("desde", inicio);
        modelo.addAttribute("hasta", fin);
        try {
            modelo.addAttribute("ventas", servicio.historial(inicio, fin));
            modelo.addAttribute("masVendidos", servicio.masVendidos(inicio, fin));
        } catch (ReglaDeNegocioException e) {
            modelo.addAttribute("error", e.getMessage());
            modelo.addAttribute("ventas", java.util.List.of());
            modelo.addAttribute("masVendidos", java.util.List.of());
        }
        return "ventas/ventas";
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
