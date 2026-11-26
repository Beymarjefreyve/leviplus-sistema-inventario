package com.leviplus.inventario.repository;

import com.leviplus.acceso.model.EstadoRegistro;
import com.leviplus.inventario.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    List<Proveedor> findAllByOrderByNombreAsc();

    List<Proveedor> findAllByEstadoOrderByNombreAsc(EstadoRegistro estado);
}
