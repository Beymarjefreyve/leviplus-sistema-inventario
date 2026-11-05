package com.leviplus.inventario.model;

import com.leviplus.acceso.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "salida_inventario")
public class SalidaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_salida")
    private Integer idSalida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lote", nullable = false)
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "motivo", nullable = false, length = 15)
    private MotivoSalida motivo;

    @Column(name = "observacion", length = 200)
    private String observacion;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    protected SalidaInventario() {
    }

    public SalidaInventario(Lote lote, Usuario usuario, int cantidad, MotivoSalida motivo, String observacion) {
        this.lote = lote;
        this.usuario = usuario;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.observacion = observacion;
    }

    @PrePersist
    void alCrear() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }

    public Integer getIdSalida() {
        return idSalida;
    }

    public Lote getLote() {
        return lote;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public int getCantidad() {
        return cantidad;
    }

    public MotivoSalida getMotivo() {
        return motivo;
    }

    public String getObservacion() {
        return observacion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
