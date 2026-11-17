package com.leviplus.ventas.controller;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.ventas.model.ModalidadPedido;
import com.leviplus.ventas.model.OrigenPedido;
import com.leviplus.ventas.service.PedidoService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Pedidos (RF-29, RF-30): se registran a mano; WhatsApp solo identifica el origen, sin integración.
 * Los dos roles pueden registrarlos y consultarlos.
 */
@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private static final int FILAS_FORMULARIO = 8;

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
        PedidoForm form = new PedidoForm();
        completarFilas(form);
        return formulario(form, modelo);
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
        completarFilas(pedidoForm);
        return formulario(pedidoForm, modelo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENCARGADO_DE_VENTAS')")
    public String detalle(@PathVariable Integer id, Model modelo) {
        modelo.addAttribute("pedido", servicio.obtener(id));
        return "pedidos/pedido-detalle";
    }

    private String formulario(PedidoForm form, Model modelo) {
        modelo.addAttribute("pedidoForm", form);
        modelo.addAttribute("productos", servicio.productosActivos());
        modelo.addAttribute("clientes", servicio.clientesFrecuentes());
        modelo.addAttribute("origenes", OrigenPedido.values());
        modelo.addAttribute("modalidades", ModalidadPedido.values());
        return "pedidos/pedido-form";
    }

    /** Rellena con filas vacías hasta las que muestra el formulario. */
    private void completarFilas(PedidoForm form) {
        while (form.getLineas().size() < FILAS_FORMULARIO) {
            form.getLineas().add(new PedidoForm.Linea());
        }
    }
}
