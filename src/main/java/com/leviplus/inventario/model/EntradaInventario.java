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
@Table(name = "entrada_inventario")
public class EntradaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrada")
    private Integer idEntrada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lote", nullable = false)
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    protected EntradaInventario() {
    }

    public EntradaInventario(Lote lote, Proveedor proveedor, Usuario usuario, int cantidad) {
        this.lote = lote;
        this.proveedor = proveedor;
        this.usuario = usuario;
        this.cantidad = cantidad;
    }

    @PrePersist
    void alCrear() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }

    public Integer getIdEntrada() {
        return idEntrada;
    }

    public Lote getLote() {
        return lote;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public int getCantidad() {
        return cantidad;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
