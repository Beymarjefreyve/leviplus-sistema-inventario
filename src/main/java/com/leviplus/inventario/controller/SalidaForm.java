package com.leviplus.inventario.controller;

import com.leviplus.inventario.model.MotivoSalida;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SalidaForm {

    @NotNull(message = "Selecciona un lote")
    private Integer idLote;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor que cero")
    private Integer cantidad;

    @NotNull(message = "Selecciona el motivo")
    private MotivoSalida motivo;

    @Size(max = 200, message = "Máximo 200 caracteres")
    private String observacion;

    public Integer getIdLote() {
        return idLote;
    }

    public void setIdLote(Integer idLote) {
        this.idLote = idLote;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public MotivoSalida getMotivo() {
        return motivo;
    }

    public void setMotivo(MotivoSalida motivo) {
        this.motivo = motivo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
