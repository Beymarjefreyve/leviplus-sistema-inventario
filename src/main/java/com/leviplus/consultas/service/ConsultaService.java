package com.leviplus.consultas.service;

import com.leviplus.inventario.model.Lote;
import com.leviplus.inventario.model.Producto;
import com.leviplus.inventario.repository.LoteRepository;
import com.leviplus.inventario.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final LoteRepository lotes;
    private final ProductoRepository productos;

    public ConsultaService(LoteRepository lotes, ProductoRepository productos) {
        this.lotes = lotes;
        this.productos = productos;
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

    private Map<Integer, Long> existencias() {
        Map<Integer, Long> resultado = new HashMap<>();
        for (Object[] fila : lotes.existenciaPorProducto()) {
            resultado.put((Integer) fila[0], ((Number) fila[1]).longValue());
        }
        return resultado;
    }
}
