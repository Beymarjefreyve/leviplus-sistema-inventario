package com.leviplus.inventario.repository;

import com.leviplus.inventario.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findAllByOrderByNombreAsc();

    boolean existsByCodigoProducto(String codigoProducto);

    boolean existsByCodigoProductoAndIdProductoNot(String codigoProducto, Integer idProducto);

    /** Búsqueda por nombre, presentación o código (sin distinguir mayúsculas); consulta parametrizada. */
    @Query("select p from Producto p where lower(p.nombre) like lower(concat('%', :texto, '%')) "
            + "or lower(p.presentacion) like lower(concat('%', :texto, '%')) "
            + "or lower(p.codigoProducto) like lower(concat('%', :texto, '%')) order by p.nombre asc")
    List<Producto> buscar(@Param("texto") String texto);
}
