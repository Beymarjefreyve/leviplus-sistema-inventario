package com.leviplus.inventario.repository;

import com.leviplus.inventario.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findAllByOrderByNombreAsc();

    boolean existsByCodigoProducto(String codigoProducto);

    boolean existsByCodigoProductoAndIdProductoNot(String codigoProducto, Integer idProducto);
}
