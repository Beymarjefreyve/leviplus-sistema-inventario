package com.leviplus.inventario.repository;

import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    /**
     * Lotes vendibles (con existencia y sin vencer) del producto, el de vencimiento más próximo primero (RF-09).
     * Se bloquean para que dos ventas simultáneas no descuenten la misma existencia.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.producto = :producto and l.cantidadDisponible > 0 "
            + "and l.fechaVencimiento >= :hoy order by l.fechaVencimiento asc, l.idLote asc")
    List<Lote> vendiblesConBloqueo(@Param("producto") Producto producto, @Param("hoy") LocalDate hoy);

    /** Lotes con existencia, con su producto, para calcular las alertas de vencimiento. */
    @Query("select l from Lote l join fetch l.producto where l.cantidadDisponible > 0 order by l.fechaVencimiento asc, l.idLote asc")
    List<Lote> conExistencia();

    /** Existencia vendible (lotes sin vencer) por producto. */
    @Query("select l.producto.idProducto, sum(l.cantidadDisponible) from Lote l "
            + "where l.fechaVencimiento >= :hoy group by l.producto.idProducto")
    List<Object[]> existenciaVendiblePorProducto(@Param("hoy") LocalDate hoy);
}
