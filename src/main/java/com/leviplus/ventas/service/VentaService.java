package com.leviplus.ventas.service;

import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.repository.LoteRepository;
import com.leviplus.inventario.repository.ProductoRepository;
import com.leviplus.ventas.controller.VentaForm;
import com.leviplus.ventas.model.DetalleVenta;
import com.leviplus.ventas.model.DetalleVentaLote;
import com.leviplus.ventas.model.EstadoVenta;
import com.leviplus.ventas.model.Venta;
import com.leviplus.ventas.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VentaService {

    /** Producto disponible para vender, con su precio y la existencia vendible (lotes sin vencer). */
    public record ProductoVendible(Integer idProducto, String codigo, String nombre, String presentacion,
                                   BigDecimal precio, long existencia) {
    }

    /** Fila del ranking de productos más vendidos. */
    public record ProductoVendido(String nombre, String presentacion, long unidades) {
    }

    private final VentaRepository ventas;
    private final ProductoRepository productos;
    private final LoteRepository lotes;
    private final UsuarioRepository usuarios;

    public VentaService(VentaRepository ventas, ProductoRepository productos, LoteRepository lotes,
                        UsuarioRepository usuarios) {
        this.ventas = ventas;
        this.productos = productos;
        this.lotes = lotes;
        this.usuarios = usuarios;
    }

    /** Productos activos con existencia vendible, para el formulario de venta. */
    @Transactional(readOnly = true)
    public List<ProductoVendible> productosVendibles() {
        Map<Integer, Long> existencia = new HashMap<>();
        for (Object[] fila : lotes.existenciaVendiblePorProducto(LocalDate.now())) {
            existencia.put((Integer) fila[0], ((Number) fila[1]).longValue());
        }
        return productos.findAllByOrderByNombreAsc().stream()
                .filter(Producto::estaActivo)
                .filter(p -> existencia.getOrDefault(p.getIdProducto(), 0L) > 0)
                .map(p -> new ProductoVendible(p.getIdProducto(), p.getCodigoProducto(), p.getNombre(),
                        p.getPresentacion(), p.getPrecioVenta(), existencia.get(p.getIdProducto())))
                .toList();
    }

    /**
     * RF-08 y RF-09: registra una venta de uno o varios productos y descuenta el inventario tomando primero el
     * lote con la fecha de vencimiento más próxima. Los lotes vencidos no se venden. Todo ocurre en una
     * transacción: si algún producto no tiene existencia suficiente, no se descuenta nada.
     */
    @Transactional
    public Venta registrar(VentaForm datos, String nombreUsuario) {
        // Se acumula por producto (ordenado por id para bloquear siempre en el mismo orden).
        Map<Integer, Integer> pedido = new TreeMap<>();
        for (VentaForm.Linea linea : datos.getLineas()) {
            if (linea == null || linea.getIdProducto() == null) {
                continue;
            }
            if (linea.getCantidad() == null || linea.getCantidad() <= 0) {
                throw new ReglaDeNegocioException("Indica una cantidad mayor que cero para cada producto");
            }
            pedido.merge(linea.getIdProducto(), linea.getCantidad(), Integer::sum);
        }
        if (pedido.isEmpty()) {
            throw new ReglaDeNegocioException("Agrega al menos un producto a la venta");
        }

        Venta venta = new Venta(usuario(nombreUsuario));
        LocalDate hoy = LocalDate.now();
        for (Map.Entry<Integer, Integer> linea : pedido.entrySet()) {
            Producto producto = productos.findById(linea.getKey())
                    .orElseThrow(() -> new ReglaDeNegocioException("El producto no existe"));
            if (!producto.estaActivo()) {
                throw new ReglaDeNegocioException("El producto " + producto.getNombre() + " está inactivo");
            }
            int cantidad = linea.getValue();
            List<Lote> disponibles = lotes.vendiblesConBloqueo(producto, hoy);
            long existencia = disponibles.stream().mapToLong(Lote::getCantidadDisponible).sum();
            if (cantidad > existencia) {
                throw new ReglaDeNegocioException("Existencia insuficiente de " + producto.getNombre()
                        + " (" + producto.getPresentacion() + "): hay " + existencia + " disponibles");
            }
            DetalleVenta detalle = venta.agregarDetalle(producto, cantidad);
            int pendiente = cantidad;
            for (Lote lote : disponibles) {
                if (pendiente == 0) {
                    break;
                }
                int tomada = Math.min(pendiente, lote.getCantidadDisponible());
                lote.restar(tomada);
                detalle.tomarDeLote(lote, tomada);
                pendiente -= tomada;
            }
        }
        venta.calcularTotal();
        return ventas.save(venta);
    }

    /** RF-25: anula una venta registrada y devuelve al inventario los productos vendidos, a sus mismos lotes. */
    @Transactional
    public Venta anular(Integer idVenta, String motivo, String nombreUsuario) {
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.isEmpty()) {
            throw new ReglaDeNegocioException("Indica el motivo de la anulación");
        }
        if (motivoLimpio.length() > 200) {
            throw new ReglaDeNegocioException("El motivo no puede superar 200 caracteres");
        }
        Venta venta = ventas.findById(idVenta).orElseThrow(() -> new ReglaDeNegocioException("La venta no existe"));
        if (venta.isAnulada()) {
            throw new ReglaDeNegocioException("La venta ya está anulada");
        }
        for (DetalleVenta detalle : venta.getDetalles()) {
            for (DetalleVentaLote asignacion : detalle.getLotes()) {
                Lote lote = lotes.buscarConBloqueo(asignacion.getLote().getIdLote()).orElseThrow();
                lote.sumar(asignacion.getCantidad());
            }
        }
        venta.anular(motivoLimpio, usuario(nombreUsuario));
        return venta;
    }

    /** Venta con su detalle y los lotes de los que salió (comprobante). */
    @Transactional(readOnly = true)
    public Venta obtener(Integer idVenta) {
        Venta venta = ventas.conUsuarios(idVenta).orElseThrow(() -> new ReglaDeNegocioException("La venta no existe"));
        for (DetalleVenta detalle : venta.getDetalles()) {
            detalle.getProducto().getNombre();
            for (DetalleVentaLote asignacion : detalle.getLotes()) {
                asignacion.getLote().getCodigoLote();
            }
        }
        return venta;
    }

    /** RF-10: historial de ventas entre dos fechas (ambas incluidas), con su estado. */
    @Transactional(readOnly = true)
    public List<Venta> historial(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return ventas.entre(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    /** RF-11: productos más vendidos del periodo, sin ventas anuladas. */
    @Transactional(readOnly = true)
    public List<ProductoVendido> masVendidos(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return ventas.masVendidos(EstadoVenta.REGISTRADA, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                .stream()
                .map(f -> new ProductoVendido((String) f[0], (String) f[1], ((Number) f[2]).longValue()))
                .toList();
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaDeNegocioException("Indica la fecha inicial y la final");
        }
        if (desde.isAfter(hasta)) {
            throw new ReglaDeNegocioException("La fecha inicial no puede ser posterior a la final");
        }
    }

    private Usuario usuario(String nombreUsuario) {
        return usuarios.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ReglaDeNegocioException("Usuario no válido"));
    }
}
