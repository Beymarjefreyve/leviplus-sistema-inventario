package com.leviplus.ventas.controller;

import com.leviplus.ventas.model.ModalidadPedido;
import com.leviplus.ventas.model.OrigenPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PedidoForm {

    private Integer idCliente;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombreCliente;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9+() -]{7,20}$", message = "Teléfono inválido (7 a 20 dígitos; puede llevar +, espacios o guiones)")
    private String telefono;

    @NotNull(message = "La fecha es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha = LocalDate.now();

    @NotNull(message = "Seleccione el origen del pedido")
    private OrigenPedido origen = OrigenPedido.WHATSAPP;

    @NotNull(message = "Seleccione la modalidad")
    private ModalidadPedido modalidad = ModalidadPedido.RECOGE;

    @Size(max = 150, message = "Máximo 150 caracteres")
    private String direccion;

    @Size(max = 250, message = "Máximo 250 caracteres")
    private String observaciones;

    @Valid
    private List<Linea> lineas = new ArrayList<>();

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public OrigenPedido getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenPedido origen) {
        this.origen = origen;
    }

    public ModalidadPedido getModalidad() {
        return modalidad;
    }

    public void setModalidad(ModalidadPedido modalidad) {
        this.modalidad = modalidad;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<Linea> getLineas() {
        return lineas;
    }

    public void setLineas(List<Linea> lineas) {
        this.lineas = lineas;
    }

    /** Una fila del pedido; las filas sin producto se ignoran. */
    public static class Linea {

        private Integer idProducto;

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 100000, message = "La cantidad es demasiado grande")
        private Integer cantidad;

        public Integer getIdProducto() {
            return idProducto;
        }

        public void setIdProducto(Integer idProducto) {
            this.idProducto = idProducto;
        }

        public Integer getCantidad() {
            return cantidad;
        }

        public void setCantidad(Integer cantidad) {
            this.cantidad = cantidad;
        }
    }
}
