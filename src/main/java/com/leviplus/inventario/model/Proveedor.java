package com.leviplus.inventario.model;

import com.leviplus.acceso.model.EstadoRegistro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "correo", length = 120)
    private String correo;

    @Column(name = "estado", nullable = false, length = 10)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;

    protected Proveedor() {
    }

    public Proveedor(String nombre, String telefono, String correo) {
        actualizar(nombre, telefono, correo);
    }

    public final void actualizar(String nombre, String telefono, String correo) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.correo = correo;
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

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public EstadoRegistro getEstado() {
        return estado;
    }
}
