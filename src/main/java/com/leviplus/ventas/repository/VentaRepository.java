package com.leviplus.ventas.repository;

import com.leviplus.ventas.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Integer> {

    /** Trae usuario y quien anula: las vistas se renderizan sin sesión de Hibernate. */
    @Query("select v from Venta v join fetch v.usuario left join fetch v.usuarioAnula where v.idVenta = :id")
    Optional<Venta> conUsuarios(@Param("id") Integer id);
}
