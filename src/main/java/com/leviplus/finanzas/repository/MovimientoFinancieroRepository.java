package com.leviplus.finanzas.repository;

import com.leviplus.finanzas.model.MovimientoFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Integer> {

    /** Trae proveedor y usuario en la misma consulta: las vistas se renderizan sin sesión de Hibernate. */
    @Query("select m from MovimientoFinanciero m left join fetch m.proveedor join fetch m.usuario "
            + "order by m.fecha desc, m.idMovimiento desc")
    List<MovimientoFinanciero> findAllConDetalle();

    /** Movimientos entre dos fechas (inclusive), del más antiguo al más reciente. */
    @Query("select m from MovimientoFinanciero m left join fetch m.proveedor join fetch m.usuario "
            + "where m.fecha between :desde and :hasta order by m.fecha asc, m.idMovimiento asc")
    List<MovimientoFinanciero> entre(@org.springframework.data.repository.query.Param("desde") java.time.LocalDate desde,
                                     @org.springframework.data.repository.query.Param("hasta") java.time.LocalDate hasta);
}
