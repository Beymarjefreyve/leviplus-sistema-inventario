package com.leviplus.acceso.controller;

import com.leviplus.acceso.model.Rol;
import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.service.UsuarioService;
import com.leviplus.common.ReglaDeNegocioException;
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

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String listar(Model modelo) {
        modelo.addAttribute("usuarios", servicio.listar());
        return "acceso/usuarios";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String nuevo(Model modelo) {
        modelo.addAttribute("usuarioForm", new UsuarioForm());
        prepararFormulario(modelo, null);
        return "acceso/usuario-form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String crear(@Valid UsuarioForm usuarioForm, BindingResult errores, Model modelo,
                        RedirectAttributes aviso) {
        if (!errores.hasErrors()) {
            try {
                servicio.crear(usuarioForm);
                aviso.addFlashAttribute("ok", "Usuario registrado correctamente");
                return "redirect:/usuarios";
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        prepararFormulario(modelo, null);
        return "acceso/usuario-form";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String editar(@PathVariable Integer id, Model modelo) {
        Usuario usuario = servicio.obtener(id);
        UsuarioForm form = new UsuarioForm();
        form.setNombreCompleto(usuario.getNombreCompleto());
        form.setNombreUsuario(usuario.getNombreUsuario());
        form.setRol(usuario.getRol());
        modelo.addAttribute("usuarioForm", form);
        prepararFormulario(modelo, usuario);
        return "acceso/usuario-form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String actualizar(@PathVariable Integer id, @Valid UsuarioForm usuarioForm, BindingResult errores,
                             Model modelo, RedirectAttributes aviso) {
        if (!errores.hasErrors()) {
            try {
                servicio.actualizar(id, usuarioForm);
                aviso.addFlashAttribute("ok", "Usuario actualizado correctamente");
                return "redirect:/usuarios";
            } catch (ReglaDeNegocioException e) {
                errores.reject("negocio", e.getMessage());
            }
        }
        prepararFormulario(modelo, servicio.obtener(id));
        return "acceso/usuario-form";
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String desactivar(@PathVariable Integer id, Authentication autenticacion, RedirectAttributes aviso) {
        try {
            servicio.desactivar(id, autenticacion.getName());
            aviso.addFlashAttribute("ok", "Usuario desactivado; ya no puede ingresar al sistema");
        } catch (ReglaDeNegocioException e) {
            aviso.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String activar(@PathVariable Integer id, RedirectAttributes aviso) {
        try {
            servicio.activar(id);
            aviso.addFlashAttribute("ok", "Usuario activado; ya puede ingresar al sistema");
        } catch (ReglaDeNegocioException e) {
            aviso.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    private void prepararFormulario(Model modelo, Usuario existente) {
        modelo.addAttribute("roles", Rol.values());
        modelo.addAttribute("usuarioEditado", existente);
    }
}
