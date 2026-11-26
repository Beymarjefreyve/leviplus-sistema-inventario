package com.leviplus.ventas.controller;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.ventas.model.ModalidadPedido;
import com.leviplus.ventas.model.OrigenPedido;
import com.leviplus.ventas.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ResponseBody;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pedidos (RF-29, RF-30): se registran a mano; WhatsApp solo identifica el origen, sin integración.
 * Los dos roles pueden registrarlos y consultarlos.
 */
@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private static final int RESULTADOS_BUSQUEDA = 20;

    /** Línea del carrito con los datos que necesita la pantalla para dibujarla. */
    public record LineaCarrito(Integer idProducto, String nombre, String presentacion, String codigo,
                               java.math.BigDecimal precio, int cantidad) {
    }

    private final PedidoService servicio;

    public PedidoController(PedidoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                         @RequestParam(required = false) OrigenPedido origen, Model modelo) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(30) : desde;
        try {
            modelo.addAttribute("pedidos", servicio.consultar(inicio, fin, origen));
        } catch (ReglaDeNegocioException e) {
            modelo.addAttribute("error", e.getMessage());
            modelo.addAttribute("pedidos", java.util.List.of());
        }
        modelo.addAttribute("desde", inicio);
        modelo.addAttribute("hasta", fin);
        modelo.addAttribute("origenElegido", origen);
        modelo.addAttribute("origenes", OrigenPedido.values());
        return "pedidos/pedidos";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String nuevo(Model modelo) {
        return formulario(new PedidoForm(), modelo);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String registrar(@Valid PedidoForm pedidoForm, BindingResult errores, Authentication autenticacion,
                            Model modelo, RedirectAttributes aviso) {
        if (!errores.hasErrors()) {
            try {
                var pedido = servicio.registrar(pedidoForm, autenticacion.getName());
                aviso.addFlashAttribute("ok", "Pedido registrado correctamente");
                return "redirect:/pedidos/" + pedido.getIdPedido();
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        return formulario(pedidoForm, modelo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String detalle(@PathVariable Integer id, Model modelo) {
        modelo.addAttribute("pedido", servicio.obtener(id));
        return "pedidos/pedido-detalle";
    }

    /** Registra el pedido como venta: descuenta el inventario y lleva al comprobante de la venta. */
    @PostMapping("/{id}/registrar-venta")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String registrarVenta(@PathVariable Integer id, Authentication autenticacion, RedirectAttributes aviso) {
        try {
            var venta = servicio.registrarComoVenta(id, autenticacion.getName());
            aviso.addFlashAttribute("ok", "Pedido registrado como venta; el inventario ya se descontó");
            return "redirect:/ventas/" + venta.getIdVenta();
        } catch (ReglaDeNegocioException e) {
            aviso.addFlashAttribute("error", e.getMessage());
            return "redirect:/pedidos/" + id;
        }
    }

    /** Buscador del formulario de pedido (JSON): nombre, presentación o código; máximo 20 resultados. */
    @GetMapping("/productos")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public List<PedidoService.ProductoPedible> buscarProductos(@RequestParam(required = false) String q) {
        return servicio.buscarPedibles(q, RESULTADOS_BUSQUEDA);
    }

    private String formulario(PedidoForm form, Model modelo) {
        modelo.addAttribute("pedidoForm", form);
        modelo.addAttribute("carrito", carritoDe(form));
        modelo.addAttribute("clientes", servicio.clientesFrecuentes());
        modelo.addAttribute("origenes", OrigenPedido.values());
        modelo.addAttribute("modalidades", ModalidadPedido.values());
        return "pedidos/pedido-form";
    }

    /** Reconstruye el carrito con las líneas enviadas, para no perderlas tras un error. */
    private List<LineaCarrito> carritoDe(PedidoForm form) {
        List<Integer> ids = new ArrayList<>();
        for (PedidoForm.Linea linea : form.getLineas()) {
            if (linea != null && linea.getIdProducto() != null) {
                ids.add(linea.getIdProducto());
            }
        }
        Map<Integer, PedidoService.ProductoPedible> datos = servicio.pediblesPorIds(ids);
        List<LineaCarrito> carrito = new ArrayList<>();
        for (PedidoForm.Linea linea : form.getLineas()) {
            if (linea == null || linea.getIdProducto() == null || !datos.containsKey(linea.getIdProducto())) {
                continue;
            }
            PedidoService.ProductoPedible p = datos.get(linea.getIdProducto());
            carrito.add(new LineaCarrito(p.idProducto(), p.nombre(), p.presentacion(), p.codigo(), p.precio(),
                    linea.getCantidad() == null ? 1 : linea.getCantidad()));
        }
        return carrito;
    }
}
