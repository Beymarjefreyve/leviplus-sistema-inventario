package com.leviplus.inventario.repository;

import com.leviplus.inventario.model.EntradaInventario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntradaInventarioRepository extends JpaRepository<EntradaInventario, Integer> {
}
