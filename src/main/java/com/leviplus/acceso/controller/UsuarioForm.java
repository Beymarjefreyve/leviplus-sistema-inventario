package com.leviplus.acceso.controller;

import com.leviplus.acceso.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UsuarioForm {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "Entre 3 y 50 caracteres")
    @Pattern(regexp = "[A-Za-z0-9._-]*", message = "Solo letras, números, punto, guion y guion bajo")
    private String nombreUsuario;

    /** Obligatoria al crear; al editar, si se deja vacía se conserva la actual. */
    @Size(max = 72, message = "Máximo 72 caracteres")
    private String contrasena;

    @NotNull(message = "Selecciona un rol")
    private Rol rol;

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }
}
