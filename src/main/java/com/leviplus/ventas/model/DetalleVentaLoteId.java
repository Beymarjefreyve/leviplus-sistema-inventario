package com.leviplus.ventas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DetalleVentaLoteId implements Serializable {

    @Column(name = "id_detalle_venta")
    private Integer idDetalleVenta;

    @Column(name = "id_lote")
    private Integer idLote;

    protected DetalleVentaLoteId() {
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof DetalleVentaLoteId id)) {
            return false;
        }
        return Objects.equals(idDetalleVenta, id.idDetalleVenta) && Objects.equals(idLote, id.idLote);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDetalleVenta, idLote);
    }
}
