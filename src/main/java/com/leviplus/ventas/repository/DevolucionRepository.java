package com.leviplus.ventas.repository;

import com.leviplus.ventas.model.Devolucion;
import com.leviplus.ventas.model.EstadoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DevolucionRepository extends JpaRepository<Devolucion, Integer> {

    @Query("select d from Devolucion d join fetch d.usuario where d.venta.idVenta = :idVenta "
            + "order by d.fechaHora asc, d.idDevolucion asc")
    List<Devolucion> deLaVenta(@Param("idVenta") Integer idVenta);

    /** Unidades ya devueltas de una venta, por detalle y lote: {idDetalleVenta, idLote, cantidad}. */
    @Query("select dd.detalleVenta.idDetalleVenta, dd.lote.idLote, sum(dd.cantidad) from DetalleDevolucion dd "
            + "where dd.devolucion.venta.idVenta = :idVenta group by dd.detalleVenta.idDetalleVenta, dd.lote.idLote")
    List<Object[]> devueltoDeLaVenta(@Param("idVenta") Integer idVenta);

    /** Valor devuelto en el intervalo [desde, hasta) de ventas que siguen registradas. */
    @Query("select sum(d.valorDevuelto) from Devolucion d where d.venta.estado = :estado "
            + "and d.fechaHora >= :desde and d.fechaHora < :hasta")
    java.math.BigDecimal valorDevuelto(@Param("estado") EstadoVenta estado, @Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta);

    /** Unidades devueltas por producto, de ventas registradas hechas en el intervalo [desde, hasta). */
    @Query("select dd.detalleVenta.producto.idProducto, sum(dd.cantidad) from DetalleDevolucion dd "
            + "where dd.devolucion.venta.estado = :estado and dd.devolucion.venta.fechaHora >= :desde "
            + "and dd.devolucion.venta.fechaHora < :hasta group by dd.detalleVenta.producto.idProducto")
    List<Object[]> devueltoPorProducto(@Param("estado") EstadoVenta estado, @Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta);
}
