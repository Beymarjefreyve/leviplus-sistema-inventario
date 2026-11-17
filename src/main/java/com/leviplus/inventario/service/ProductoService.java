package com.leviplus.inventario.service;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.controller.ProductoForm;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.repository.LoteRepository;
import com.leviplus.inventario.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductoService {

    private final ProductoRepository productos;
    private final LoteRepository lotes;

    public ProductoService(ProductoRepository productos, LoteRepository lotes) {
        this.productos = productos;
        this.lotes = lotes;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return productos.findAllByOrderByNombreAsc();
    }

    /**
     * Búsqueda para los listados: por nombre, presentación o código, con filtro de estado ("activos", "inactivos"
     * o cualquier otro valor para todos). Devuelve a lo sumo {@code limite} filas para no cargar catálogos enormes.
     */
    @Transactional(readOnly = true)
    public List<Producto> buscar(String texto, String estado, int limite) {
        List<Producto> base = texto == null || texto.isBlank() ? productos.findAllByOrderByNombreAsc()
                : productos.buscar(texto.trim());
        return base.stream()
                .filter(p -> "activos".equals(estado) ? p.estaActivo()
                        : !"inactivos".equals(estado) || !p.estaActivo())
                .limit(limite)
                .toList();
    }

    /** Existencia total (suma de los lotes) por id de producto; sin lotes equivale a cero. */
    @Transactional(readOnly = true)
    public Map<Integer, Long> existencias() {
        Map<Integer, Long> resultado = new HashMap<>();
        for (Object[] fila : lotes.existenciaPorProducto()) {
            resultado.put((Integer) fila[0], ((Number) fila[1]).longValue());
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public Producto obtener(Integer id) {
        return productos.findById(id).orElseThrow(() -> new ReglaDeNegocioException("El producto no existe"));
    }

    /** RF-03. */
    @Transactional
    public Producto crear(ProductoForm datos) {
        String codigo = datos.getCodigoProducto().trim();
        if (productos.existsByCodigoProducto(codigo)) {
            throw new ReglaDeNegocioException("Ya existe un producto con ese código");
        }
        return productos.save(new Producto(codigo, datos.getNombre().trim(), datos.getPresentacion().trim(),
                datos.getLaboratorio().trim(), datos.getPrecioVenta(), datos.getStockMinimo(),
                datos.getDiasAlertaVencimiento()));
    }

    /** RF-23. */
    @Transactional
    public Producto actualizar(Integer id, ProductoForm datos) {
        Producto producto = obtener(id);
        String codigo = datos.getCodigoProducto().trim();
        if (productos.existsByCodigoProductoAndIdProductoNot(codigo, id)) {
            throw new ReglaDeNegocioException("Ya existe otro producto con ese código");
        }
        producto.actualizar(codigo, datos.getNombre().trim(), datos.getPresentacion().trim(),
                datos.getLaboratorio().trim(), datos.getPrecioVenta(), datos.getStockMinimo(),
                datos.getDiasAlertaVencimiento());
        return productos.save(producto);
    }

    /** RF-24: conserva el historial; deja de estar disponible en nuevas entradas y ventas. */
    @Transactional
    public void inactivar(Integer id) {
        Producto producto = obtener(id);
        if (!producto.estaActivo()) {
            throw new ReglaDeNegocioException("El producto ya está inactivo");
        }
        producto.inactivar();
        productos.save(producto);
    }

    /** Vuelve a dejar disponible un producto inactivo; su historial y sus lotes no cambian. */
    @Transactional
    public void reactivar(Integer id) {
        Producto producto = obtener(id);
        if (producto.estaActivo()) {
            throw new ReglaDeNegocioException("El producto ya está activo");
        }
        producto.reactivar();
        productos.save(producto);
    }
}
