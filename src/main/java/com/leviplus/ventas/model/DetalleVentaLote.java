package com.leviplus.ventas.model;

import com.leviplus.inventario.model.Lote;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/** Cantidad de un detalle de venta que salió de un lote concreto. */
@Entity
@Table(name = "detalle_venta_lote")
public class DetalleVentaLote {

    @EmbeddedId
    private DetalleVentaLoteId id = new DetalleVentaLoteId();

    @MapsId("idDetalleVenta")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_detalle_venta")
    private DetalleVenta detalle;

    @MapsId("idLote")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lote")
    private Lote lote;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    protected DetalleVentaLote() {
    }

    DetalleVentaLote(DetalleVenta detalle, Lote lote, int cantidad) {
        this.detalle = detalle;
        this.lote = lote;
        this.cantidad = cantidad;
    }

    public DetalleVenta getDetalle() {
        return detalle;
    }

    public Lote getLote() {
        return lote;
    }

    public int getCantidad() {
        return cantidad;
    }
}
