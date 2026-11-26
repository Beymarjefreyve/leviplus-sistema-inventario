package com.leviplus.inventario.service;

import com.leviplus.acceso.model.EstadoRegistro;
import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.controller.ProveedorForm;
import com.leviplus.inventario.model.Proveedor;
import com.leviplus.inventario.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedores;

    public ProveedorService(ProveedorRepository proveedores) {
        this.proveedores = proveedores;
    }

    /** RF-22. */
    @Transactional(readOnly = true)
    public List<Proveedor> listar() {
        return proveedores.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listarActivos() {
        return proveedores.findAllByEstadoOrderByNombreAsc(EstadoRegistro.ACTIVO);
    }

    @Transactional(readOnly = true)
    public Proveedor obtener(Integer id) {
        return proveedores.findById(id).orElseThrow(() -> new ReglaDeNegocioException("El proveedor no existe"));
    }

    /** RF-21: el proveedor queda activo. */
    @Transactional
    public Proveedor crear(ProveedorForm datos) {
        return proveedores.save(new Proveedor(datos.getNombre().trim(), datos.getTelefono().trim(),
                correoONulo(datos.getCorreo())));
    }

    @Transactional
    public Proveedor actualizar(Integer id, ProveedorForm datos) {
        Proveedor proveedor = obtener(id);
        proveedor.actualizar(datos.getNombre().trim(), datos.getTelefono().trim(), correoONulo(datos.getCorreo()));
        return proveedores.save(proveedor);
    }

    @Transactional
    public void inactivar(Integer id) {
        Proveedor proveedor = obtener(id);
        if (!proveedor.estaActivo()) {
            throw new ReglaDeNegocioException("El proveedor ya está inactivo");
        }
        proveedor.inactivar();
        proveedores.save(proveedor);
    }

    @Transactional
    public void reactivar(Integer id) {
        Proveedor proveedor = obtener(id);
        if (proveedor.estaActivo()) {
            throw new ReglaDeNegocioException("El proveedor ya está activo");
        }
        proveedor.reactivar();
        proveedores.save(proveedor);
    }

    private String correoONulo(String correo) {
        return correo == null || correo.isBlank() ? null : correo.trim();
    }
}
