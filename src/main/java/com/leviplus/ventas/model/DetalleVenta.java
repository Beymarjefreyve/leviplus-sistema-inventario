package com.leviplus.ventas.model;

import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "detalle_venta")
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_venta")
    private Integer idDetalleVenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    /** Lotes de los que salió la cantidad vendida (primero el de vencimiento más próximo). */
    @OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL)
    @jakarta.persistence.OrderBy("id.idLote ASC")
    private List<DetalleVentaLote> lotes = new ArrayList<>();

    protected DetalleVenta() {
    }

    DetalleVenta(Venta venta, Producto producto, int cantidad) {
        this.venta = venta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecioVenta();
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public void tomarDeLote(Lote lote, int cantidadTomada) {
        lotes.add(new DetalleVentaLote(this, lote, cantidadTomada));
    }

    public Integer getIdDetalleVenta() {
        return idDetalleVenta;
    }

    public Venta getVenta() {
        return venta;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public List<DetalleVentaLote> getLotes() {
        return lotes;
    }
}
