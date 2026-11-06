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
}
