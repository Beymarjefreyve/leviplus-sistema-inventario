package com.leviplus.inventario.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductoForm {

    @NotBlank(message = "El código del producto es obligatorio")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String codigoProducto;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    @NotBlank(message = "La presentación es obligatoria")
    @Size(max = 80, message = "Máximo 80 caracteres")
    private String presentacion;

    @NotBlank(message = "El laboratorio es obligatorio")
    @Size(max = 80, message = "Máximo 80 caracteres")
    private String laboratorio;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "Precio inválido (máximo 2 decimales)")
    private BigDecimal precioVenta;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "No puede ser negativo")
    private Integer stockMinimo;

    @NotNull(message = "Los días de alerta son obligatorios")
    @Min(value = 0, message = "No puede ser negativo")
    private Integer diasAlertaVencimiento;

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion;
    }

    public String getLaboratorio() {
        return laboratorio;
    }

    public void setLaboratorio(String laboratorio) {
        this.laboratorio = laboratorio;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public Integer getDiasAlertaVencimiento() {
        return diasAlertaVencimiento;
    }

    public void setDiasAlertaVencimiento(Integer diasAlertaVencimiento) {
        this.diasAlertaVencimiento = diasAlertaVencimiento;
    }
}
