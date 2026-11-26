package com.leviplus.inventario.repository;

import com.leviplus.inventario.model.SalidaInventario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalidaInventarioRepository extends JpaRepository<SalidaInventario, Integer> {
}
