package com.leviplus.ventas.model;

import com.leviplus.acceso.model.Usuario;
import com.leviplus.inventario.model.Lote;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** RF-26: devolución (total o parcial) de productos de una venta registrada. */
@Entity
@Table(name = "devolucion")
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_devolucion")
    private Integer idDevolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(name = "motivo", nullable = false, length = 200)
    private String motivo;

    @Column(name = "valor_devuelto", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorDevuelto = BigDecimal.ZERO;

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL)
    private List<DetalleDevolucion> detalles = new ArrayList<>();

    protected Devolucion() {
    }

    public Devolucion(Venta venta, Usuario usuario, String motivo) {
        this.venta = venta;
        this.usuario = usuario;
        this.motivo = motivo;
    }

    @PrePersist
    void alCrear() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }

    public void agregar(DetalleVenta detalleVenta, Lote lote, int cantidad, DestinoDevolucion destino) {
        BigDecimal valor = detalleVenta.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad));
        detalles.add(new DetalleDevolucion(this, detalleVenta, lote, cantidad, destino, valor));
        valorDevuelto = valorDevuelto.add(valor);
    }

    public Integer getIdDevolucion() {
        return idDevolucion;
    }

    public Venta getVenta() {
        return venta;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getMotivo() {
        return motivo;
    }

    public BigDecimal getValorDevuelto() {
        return valorDevuelto;
    }

    public List<DetalleDevolucion> getDetalles() {
        return detalles;
    }
}
