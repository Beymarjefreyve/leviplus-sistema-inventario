package com.leviplus.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "lote", uniqueConstraints = @UniqueConstraint(columnNames = {"id_producto", "codigo_lote"}))
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lote")
    private Integer idLote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "codigo_lote", nullable = false, length = 40)
    private String codigoLote;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "cantidad_disponible", nullable = false)
    private int cantidadDisponible;

    protected Lote() {
    }

    public Lote(Producto producto, String codigoLote, LocalDate fechaVencimiento) {
        this.producto = producto;
        this.codigoLote = codigoLote;
        this.fechaVencimiento = fechaVencimiento;
    }

    public void sumar(int cantidad) {
        this.cantidadDisponible += cantidad;
    }

    public void restar(int cantidad) {
        if (cantidad > cantidadDisponible) {
            throw new IllegalStateException("La cantidad excede la existencia del lote");
        }
        this.cantidadDisponible -= cantidad;
    }

    public Integer getIdLote() {
        return idLote;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }
}
