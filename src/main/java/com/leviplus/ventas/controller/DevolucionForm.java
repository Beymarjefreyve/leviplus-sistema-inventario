package com.leviplus.ventas.controller;

import com.leviplus.ventas.model.DestinoDevolucion;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class DevolucionForm {

    @Size(max = 200, message = "Máximo 200 caracteres")
    private String motivo;

    private List<Linea> lineas = new ArrayList<>();

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public List<Linea> getLineas() {
        return lineas;
    }

    public void setLineas(List<Linea> lineas) {
        this.lineas = lineas;
    }

    /** Un producto de la venta: cuántas unidades se devuelven y qué se hace con ellas. */
    public static class Linea {

        private Integer idDetalleVenta;
        private Integer cantidad;
        private DestinoDevolucion destino = DestinoDevolucion.REINTEGRO;

        public Integer getIdDetalleVenta() {
            return idDetalleVenta;
        }

        public void setIdDetalleVenta(Integer idDetalleVenta) {
            this.idDetalleVenta = idDetalleVenta;
        }

        public Integer getCantidad() {
            return cantidad;
        }

        public void setCantidad(Integer cantidad) {
            this.cantidad = cantidad;
        }

        public DestinoDevolucion getDestino() {
            return destino;
        }

        public void setDestino(DestinoDevolucion destino) {
            this.destino = destino;
        }
    }
}
