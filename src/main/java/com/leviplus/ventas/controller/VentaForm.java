package com.leviplus.ventas.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.ArrayList;
import java.util.List;

public class VentaForm {

    @Valid
    private List<Linea> lineas = new ArrayList<>();

    public List<Linea> getLineas() {
        return lineas;
    }

    public void setLineas(List<Linea> lineas) {
        this.lineas = lineas;
    }

    /** Una fila de la venta; las filas sin producto se ignoran. */
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
