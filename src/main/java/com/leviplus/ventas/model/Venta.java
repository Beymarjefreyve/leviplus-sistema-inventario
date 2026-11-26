package com.leviplus.ventas.model;

import com.leviplus.acceso.model.Usuario;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Integer idVenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 12)
    private EstadoVenta estado = EstadoVenta.REGISTRADA;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    @Column(name = "fecha_hora_anulacion")
    private LocalDateTime fechaHoraAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anula")
    private Usuario usuarioAnula;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<DetalleVenta> detalles = new ArrayList<>();

    protected Venta() {
    }

    public Venta(Usuario usuario) {
        this.usuario = usuario;
    }

    @PrePersist
    void alCrear() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }

    public DetalleVenta agregarDetalle(Producto producto, int cantidad) {
        DetalleVenta detalle = new DetalleVenta(this, producto, cantidad);
        detalles.add(detalle);
        return detalle;
    }

    public void calcularTotal() {
        total = detalles.stream().map(DetalleVenta::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isAnulada() {
        return estado == EstadoVenta.ANULADA;
    }

    public void anular(String motivo, Usuario quienAnula) {
        this.estado = EstadoVenta.ANULADA;
        this.motivoAnulacion = motivo;
        this.fechaHoraAnulacion = LocalDateTime.now();
        this.usuarioAnula = quienAnula;
    }

    public Integer getIdVenta() {
        return idVenta;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public LocalDateTime getFechaHoraAnulacion() {
        return fechaHoraAnulacion;
    }

    public Usuario getUsuarioAnula() {
        return usuarioAnula;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }
}
