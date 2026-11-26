package com.leviplus.consultas.service;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.repository.LoteRepository;
import com.leviplus.inventario.repository.ProductoRepository;
import com.leviplus.ventas.model.EstadoVenta;
import com.leviplus.ventas.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Consultas del módulo 5: alertas de vencimiento y bajo stock, existencias y panel de indicadores. */
@Service
public class ConsultaService {

    /** Lote que vence dentro de los días de alerta de su producto; {@code diasRestantes} negativo = ya venció. */
    public record AlertaVencimiento(String producto, String presentacion, String codigoLote,
                                    LocalDate fechaVencimiento, int cantidad, long diasRestantes) {
        public boolean vencido() {
            return diasRestantes < 0;
        }
    }

    public record AlertaBajoStock(String producto, String presentacion, String codigo, long existencia,
                                  int stockMinimo) {
    }

    public record Existencia(Integer idProducto, String codigo, String nombre, String presentacion,
                             String laboratorio, long existencia, boolean activo) {
    }

    /**
     * Lote de un producto en la consulta de detalle. {@code orden} es 1 para el lote del que se descuenta
     * primero en una venta, 2 para el siguiente, etc.; 0 si no es vendible (vencido o sin existencia).
     */
    public record LoteDetalle(String codigoLote, LocalDate fechaVencimiento, int disponible, long diasRestantes,
                              boolean porVencer, int orden) {
        public boolean vencido() {
            return diasRestantes < 0;
        }
    }

    public record DetalleExistencia(Existencia producto, int stockMinimo, List<LoteDetalle> lotes) {
    }

    public record Panel(long ventasHoy, BigDecimal totalVentasHoy, int lotesPorVencer, int productosBajoStock) {
    }

    private final LoteRepository lotes;
    private final ProductoRepository productos;
    private final VentaRepository ventas;

    public ConsultaService(LoteRepository lotes, ProductoRepository productos, VentaRepository ventas) {
        this.lotes = lotes;
        this.productos = productos;
        this.ventas = ventas;
    }

    /**
     * RF-06 y RF-17: lotes con existencia cuyo vencimiento cae dentro de los días de alerta del producto
     * (incluye los ya vencidos), del más próximo al más lejano.
     */
    @Transactional(readOnly = true)
    public List<AlertaVencimiento> lotesPorVencer() {
        LocalDate hoy = LocalDate.now();
        return lotes.conExistencia().stream()
                .filter(l -> !l.getFechaVencimiento().isAfter(hoy.plusDays(l.getProducto().getDiasAlertaVencimiento())))
                .map(l -> alerta(l, hoy))
                .toList();
    }

    private AlertaVencimiento alerta(Lote lote, LocalDate hoy) {
        Producto p = lote.getProducto();
        return new AlertaVencimiento(p.getNombre(), p.getPresentacion(), lote.getCodigoLote(),
                lote.getFechaVencimiento(), lote.getCantidadDisponible(),
                ChronoUnit.DAYS.between(hoy, lote.getFechaVencimiento()));
    }

    /** RF-07: productos activos cuya existencia total (suma de lotes) es menor que su stock mínimo. */
    @Transactional(readOnly = true)
    public List<AlertaBajoStock> productosBajoStock() {
        Map<Integer, Long> existencia = existencias();
        return productos.findAllByOrderByNombreAsc().stream()
                .filter(Producto::estaActivo)
                .filter(p -> existencia.getOrDefault(p.getIdProducto(), 0L) < p.getStockMinimo())
                .map(p -> new AlertaBajoStock(p.getNombre(), p.getPresentacion(), p.getCodigoProducto(),
                        existencia.getOrDefault(p.getIdProducto(), 0L), p.getStockMinimo()))
                .toList();
    }

    /** RF-18: existencia actual por nombre, presentación o código; sin texto lista todos los productos. */
    @Transactional(readOnly = true)
    public List<Existencia> buscarExistencia(String texto) {
        String criterio = texto == null ? "" : texto.trim();
        Map<Integer, Long> existencia = existencias();
        List<Producto> encontrados = criterio.isEmpty() ? productos.findAllByOrderByNombreAsc() : productos.buscar(criterio);
        return encontrados.stream()
                .map(p -> new Existencia(p.getIdProducto(), p.getCodigoProducto(), p.getNombre(), p.getPresentacion(),
                        p.getLaboratorio(), existencia.getOrDefault(p.getIdProducto(), 0L), p.estaActivo()))
                .toList();
    }

    /**
     * Desglose de lotes de un producto, en el mismo orden en que las ventas los descuentan
     * (vencimiento más próximo primero; los vencidos no se venden).
     */
    @Transactional(readOnly = true)
    public DetalleExistencia detalleExistencia(Integer idProducto) {
        Producto p = productos.findById(idProducto)
                .orElseThrow(() -> new ReglaDeNegocioException("El producto no existe"));
        LocalDate hoy = LocalDate.now();
        List<Lote> delProducto = lotes.findByProductoOrderByFechaVencimientoAsc(p);
        int[] siguiente = {1};
        List<LoteDetalle> detalle = delProducto.stream()
                .sorted(java.util.Comparator.comparing(Lote::getFechaVencimiento).thenComparing(Lote::getIdLote))
                .map(l -> {
                    long dias = ChronoUnit.DAYS.between(hoy, l.getFechaVencimiento());
                    boolean vendible = dias >= 0 && l.getCantidadDisponible() > 0;
                    boolean porVencer = dias >= 0 && dias <= p.getDiasAlertaVencimiento();
                    return new LoteDetalle(l.getCodigoLote(), l.getFechaVencimiento(), l.getCantidadDisponible(),
                            dias, porVencer, vendible ? siguiente[0]++ : 0);
                })
                .toList();
        long total = delProducto.stream().mapToLong(Lote::getCantidadDisponible).sum();
        Existencia existencia = new Existencia(p.getIdProducto(), p.getCodigoProducto(), p.getNombre(),
                p.getPresentacion(), p.getLaboratorio(), total, p.estaActivo());
        return new DetalleExistencia(existencia, p.getStockMinimo(), detalle);
    }

    /** RF-15: indicadores del día (ventas registradas, sin las anuladas) y alertas vigentes. */
    @Transactional(readOnly = true)
    public Panel panel() {
        LocalDate hoy = LocalDate.now();
        Object[] fila = ventas.resumen(EstadoVenta.REGISTRADA, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay()).get(0);
        long cantidad = ((Number) fila[0]).longValue();
        BigDecimal total = fila[1] == null ? BigDecimal.ZERO : (BigDecimal) fila[1];
        return new Panel(cantidad, total, lotesPorVencer().size(), productosBajoStock().size());
    }

    private Map<Integer, Long> existencias() {
        Map<Integer, Long> resultado = new HashMap<>();
        for (Object[] fila : lotes.existenciaPorProducto()) {
            resultado.put((Integer) fila[0], ((Number) fila[1]).longValue());
        }
        return resultado;
    }
}
