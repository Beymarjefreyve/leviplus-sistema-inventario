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
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-29 y RF-30: pedido registrado manualmente (WhatsApp, llamada o presencial), para recoger o a domicilio.
 * No genera venta ni mueve inventario.
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private ClienteFrecuente cliente;

    @Column(name = "nombre_cliente", nullable = false, length = 100)
    private String nombreCliente;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "origen", nullable = false, length = 12)
    private OrigenPedido origen;

    @Column(name = "modalidad", nullable = false, length = 10)
    private ModalidadPedido modalidad;

    @Column(name = "direccion", length = 150)
    private String direccion;

    @Column(name = "observaciones", length = 250)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    @OrderBy("idDetallePedido ASC")
    private List<DetallePedido> detalles = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(ClienteFrecuente cliente, String nombreCliente, String telefono, LocalDate fecha,
                  OrigenPedido origen, ModalidadPedido modalidad, String direccion, String observaciones,
                  Usuario usuario) {
        this.cliente = cliente;
        this.nombreCliente = nombreCliente;
        this.telefono = telefono;
        this.fecha = fecha;
        this.origen = origen;
        this.modalidad = modalidad;
        this.direccion = direccion;
        this.observaciones = observaciones;
        this.usuario = usuario;
    }

    @PrePersist
    void alCrear() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    public void agregarDetalle(Producto producto, int cantidad) {
        detalles.add(new DetallePedido(this, producto, cantidad));
    }

    public Integer getIdPedido() {
        return idPedido;
    }

    public ClienteFrecuente getCliente() {
        return cliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public OrigenPedido getOrigen() {
        return origen;
    }

    public ModalidadPedido getModalidad() {
        return modalidad;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }
}
