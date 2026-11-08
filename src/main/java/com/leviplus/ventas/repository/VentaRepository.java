package com.leviplus.ventas.repository;

import com.leviplus.ventas.model.EstadoVenta;
import com.leviplus.ventas.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Integer> {

    /** Ventas del intervalo [desde, hasta); trae el usuario porque las vistas se renderizan sin sesión de Hibernate. */
    @Query("select v from Venta v join fetch v.usuario where v.fechaHora >= :desde and v.fechaHora < :hasta "
            + "order by v.fechaHora desc, v.idVenta desc")
    List<Venta> entre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Trae usuario y quien anula: las vistas se renderizan sin sesión de Hibernate. */
    @Query("select v from Venta v join fetch v.usuario left join fetch v.usuarioAnula where v.idVenta = :id")
    Optional<Venta> conUsuarios(@Param("id") Integer id);

    /** RF-11: unidades vendidas por producto en el periodo, sin contar ventas anuladas. */
    @Query("select d.producto.nombre, d.producto.presentacion, sum(d.cantidad) from DetalleVenta d "
            + "where d.venta.estado = :estado and d.venta.fechaHora >= :desde and d.venta.fechaHora < :hasta "
            + "group by d.producto.idProducto, d.producto.nombre, d.producto.presentacion "
            + "order by sum(d.cantidad) desc, d.producto.nombre asc")
    List<Object[]> masVendidos(@Param("estado") EstadoVenta estado, @Param("desde") LocalDateTime desde,
                               @Param("hasta") LocalDateTime hasta);
}
