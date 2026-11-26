package com.leviplus.ventas.repository;

import com.leviplus.ventas.model.OrigenPedido;
import com.leviplus.ventas.model.Pedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    /** Pedido con bloqueo, para que dos clics no lo conviertan en venta dos veces. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.idPedido = :id")
    Optional<Pedido> buscarConBloqueo(@Param("id") Integer id);

    /** Pedidos entre dos fechas (inclusive), con filtro opcional por origen; los más recientes primero. */
    @Query("select p from Pedido p where p.fecha between :desde and :hasta "
            + "and (:origen is null or p.origen = :origen) order by p.fecha desc, p.idPedido desc")
    List<Pedido> buscar(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                        @Param("origen") OrigenPedido origen);
}
