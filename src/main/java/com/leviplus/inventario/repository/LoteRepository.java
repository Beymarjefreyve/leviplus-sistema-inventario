package com.leviplus.inventario.repository;

import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Integer> {

    Optional<Lote> findByProductoAndCodigoLote(Producto producto, String codigoLote);

    List<Lote> findByProductoAndCantidadDisponibleGreaterThanOrderByFechaVencimientoAsc(Producto producto, int minimo);

    List<Lote> findByProductoOrderByFechaVencimientoAsc(Producto producto);

    @Query("select l.producto.idProducto, sum(l.cantidadDisponible) from Lote l group by l.producto.idProducto")
    List<Object[]> existenciaPorProducto();

    @Query("select coalesce(sum(l.cantidadDisponible), 0) from Lote l where l.producto = :producto")
    long existenciaDe(Producto producto);
}
