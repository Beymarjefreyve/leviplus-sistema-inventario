package com.leviplus.ventas.repository;

import com.leviplus.ventas.model.ClienteFrecuente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteFrecuenteRepository extends JpaRepository<ClienteFrecuente, Integer> {

    List<ClienteFrecuente> findAllByOrderByNombreAsc();
}
