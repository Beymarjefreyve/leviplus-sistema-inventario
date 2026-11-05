package com.leviplus.inventario.service;

import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.controller.EntradaForm;
import com.leviplus.inventario.controller.SalidaForm;
import com.leviplus.inventario.model.EntradaInventario;
import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.model.Proveedor;
import com.leviplus.inventario.model.SalidaInventario;
import com.leviplus.inventario.repository.EntradaInventarioRepository;
import com.leviplus.inventario.repository.LoteRepository;
import com.leviplus.inventario.repository.ProductoRepository;
import com.leviplus.inventario.repository.ProveedorRepository;
import com.leviplus.inventario.repository.SalidaInventarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class InventarioService {

    private final ProductoRepository productos;
    private final ProveedorRepository proveedores;
    private final LoteRepository lotes;
    private final EntradaInventarioRepository entradas;
    private final SalidaInventarioRepository salidas;
    private final UsuarioRepository usuarios;

    public InventarioService(ProductoRepository productos, ProveedorRepository proveedores, LoteRepository lotes,
                             EntradaInventarioRepository entradas, SalidaInventarioRepository salidas,
                             UsuarioRepository usuarios) {
        this.productos = productos;
        this.proveedores = proveedores;
        this.lotes = lotes;
        this.entradas = entradas;
        this.salidas = salidas;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public long existenciaDe(Integer idProducto) {
        return lotes.existenciaDe(producto(idProducto));
    }

    @Transactional(readOnly = true)
    public List<Lote> lotesDisponibles(Integer idProducto) {
        return lotes.findByProductoAndCantidadDisponibleGreaterThanOrderByFechaVencimientoAsc(producto(idProducto), 0);
    }

    @Transactional(readOnly = true)
    public List<Lote> lotesDe(Integer idProducto) {
        return lotes.findByProductoOrderByFechaVencimientoAsc(producto(idProducto));
    }

    /**
     * RF-04: registra la entrada de un lote. Si el lote ya existe para el producto se le suma la cantidad
     * (la fecha de vencimiento debe coincidir); si no, se crea.
     */
    @Transactional
    public EntradaInventario registrarEntrada(Integer idProducto, EntradaForm datos, String nombreUsuario) {
        Producto producto = producto(idProducto);
        if (!producto.estaActivo()) {
            throw new ReglaDeNegocioException("El producto está inactivo y no admite nuevas entradas");
        }
        Proveedor proveedor = proveedores.findById(datos.getIdProveedor())
                .orElseThrow(() -> new ReglaDeNegocioException("El proveedor no existe"));
        if (!proveedor.estaActivo()) {
            throw new ReglaDeNegocioException("El proveedor está inactivo");
        }
        if (datos.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new ReglaDeNegocioException("La fecha de vencimiento no puede estar en el pasado");
        }

        String codigoLote = datos.getCodigoLote().trim();
        Lote lote = lotes.findByProductoAndCodigoLote(producto, codigoLote)
                .orElseGet(() -> lotes.save(new Lote(producto, codigoLote, datos.getFechaVencimiento())));
        if (!lote.getFechaVencimiento().equals(datos.getFechaVencimiento())) {
            throw new ReglaDeNegocioException("El lote " + codigoLote + " ya existe con vencimiento "
                    + lote.getFechaVencimiento() + "; usa esa fecha o un código de lote distinto");
        }
        lote.sumar(datos.getCantidad());
        lotes.save(lote);
        return entradas.save(new EntradaInventario(lote, proveedor, usuario(nombreUsuario), datos.getCantidad()));
    }

    /** RF-05: salida no asociada a una venta; no puede exceder la existencia del lote. */
    @Transactional
    public SalidaInventario registrarSalida(Integer idProducto, SalidaForm datos, String nombreUsuario) {
        Producto producto = producto(idProducto);
        Lote lote = lotes.findById(datos.getIdLote())
                .filter(l -> l.getProducto().getIdProducto().equals(producto.getIdProducto()))
                .orElseThrow(() -> new ReglaDeNegocioException("El lote no pertenece a este producto"));
        if (datos.getCantidad() > lote.getCantidadDisponible()) {
            throw new ReglaDeNegocioException("La cantidad excede la existencia del lote ("
                    + lote.getCantidadDisponible() + " disponibles)");
        }
        lote.restar(datos.getCantidad());
        lotes.save(lote);
        String observacion = datos.getObservacion() == null || datos.getObservacion().isBlank()
                ? null : datos.getObservacion().trim();
        return salidas.save(new SalidaInventario(lote, usuario(nombreUsuario), datos.getCantidad(),
                datos.getMotivo(), observacion));
    }

    private Producto producto(Integer id) {
        return productos.findById(id).orElseThrow(() -> new ReglaDeNegocioException("El producto no existe"));
    }

    private Usuario usuario(String nombreUsuario) {
        return usuarios.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ReglaDeNegocioException("Usuario no válido"));
    }
}
