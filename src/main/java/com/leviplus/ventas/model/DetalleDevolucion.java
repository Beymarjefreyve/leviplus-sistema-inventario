package com.leviplus.ventas.model;

import com.leviplus.inventario.model.Lote;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_devolucion")
public class DetalleDevolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_devolucion")
    private Integer idDetalleDevolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_devolucion", nullable = false)
    private Devolucion devolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_detalle_venta", nullable = false)
    private DetalleVenta detalleVenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lote", nullable = false)
    private Lote lote;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "destino", nullable = false, length = 10)
    private DestinoDevolucion destino;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    protected DetalleDevolucion() {
    }

    DetalleDevolucion(Devolucion devolucion, DetalleVenta detalleVenta, Lote lote, int cantidad,
                      DestinoDevolucion destino, BigDecimal valor) {
        this.devolucion = devolucion;
        this.detalleVenta = detalleVenta;
        this.lote = lote;
        this.cantidad = cantidad;
        this.destino = destino;
        this.valor = valor;
    }

    public Integer getIdDetalleDevolucion() {
        return idDetalleDevolucion;
    }

    public Devolucion getDevolucion() {
        return devolucion;
    }

    public DetalleVenta getDetalleVenta() {
        return detalleVenta;
    }

    public Lote getLote() {
        return lote;
    }

    public int getCantidad() {
        return cantidad;
    }

    public DestinoDevolucion getDestino() {
        return destino;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
