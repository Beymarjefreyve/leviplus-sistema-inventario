package com.leviplus.inventario.model;

import com.leviplus.acceso.model.EstadoRegistro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "codigo_producto", nullable = false, unique = true, length = 50)
    private String codigoProducto;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "presentacion", nullable = false, length = 80)
    private String presentacion;

    @Column(name = "laboratorio", nullable = false, length = 80)
    private String laboratorio;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;

    @Column(name = "dias_alerta_vencimiento", nullable = false)
    private int diasAlertaVencimiento;

    @Column(name = "estado", nullable = false, length = 10)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    protected Producto() {
    }

    public Producto(String codigoProducto, String nombre, String presentacion, String laboratorio,
                    BigDecimal precioVenta, int stockMinimo, int diasAlertaVencimiento) {
        actualizar(codigoProducto, nombre, presentacion, laboratorio, precioVenta, stockMinimo, diasAlertaVencimiento);
    }

    @PrePersist
    void alCrear() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    public final void actualizar(String codigoProducto, String nombre, String presentacion, String laboratorio,
                                 BigDecimal precioVenta, int stockMinimo, int diasAlertaVencimiento) {
        this.codigoProducto = codigoProducto;
        this.nombre = nombre;
        this.presentacion = presentacion;
        this.laboratorio = laboratorio;
        this.precioVenta = precioVenta;
        this.stockMinimo = stockMinimo;
        this.diasAlertaVencimiento = diasAlertaVencimiento;
    }

    public boolean estaActivo() {
        return estado == EstadoRegistro.ACTIVO;
    }

    public void inactivar() {
        this.estado = EstadoRegistro.INACTIVO;
    }

    public void reactivar() {
        this.estado = EstadoRegistro.ACTIVO;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public String getLaboratorio() {
        return laboratorio;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public int getDiasAlertaVencimiento() {
        return diasAlertaVencimiento;
    }

    public EstadoRegistro getEstado() {
        return estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
