package com.leviplus.consultas.service;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.finanzas.model.MovimientoFinanciero;
import com.leviplus.finanzas.model.TipoMovimiento;
import com.leviplus.finanzas.repository.MovimientoFinancieroRepository;
import com.leviplus.ventas.model.EstadoVenta;
import com.leviplus.ventas.model.Venta;
import com.leviplus.ventas.repository.DevolucionRepository;
import com.leviplus.ventas.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-14 (balance del periodo) y RF-16 (reportes de ventas y financieros, exportables a CSV).
 * Las ventas anuladas no cuentan y el valor devuelto se descuenta de la venta.
 */
@Service
public class ReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record FilaVenta(Integer idVenta, String fechaHora, String atendio, BigDecimal total,
                            BigDecimal devuelto, BigDecimal neto) {
    }

    public record ReporteVentas(LocalDate desde, LocalDate hasta, List<FilaVenta> filas, BigDecimal totalBruto,
                                BigDecimal totalDevuelto, BigDecimal totalNeto) {
        public boolean vacio() {
            return filas.isEmpty();
        }
    }

    public record FilaMovimiento(String fecha, String tipo, String concepto, String proveedor, BigDecimal valor) {
    }

    /** Balance = ventas netas + otros ingresos − egresos. */
    public record Balance(LocalDate desde, LocalDate hasta, BigDecimal ventasNetas, BigDecimal otrosIngresos,
                          BigDecimal egresos, BigDecimal balance) {
    }

    public record ReporteFinanciero(Balance balance, List<FilaMovimiento> movimientos) {
        public boolean vacio() {
            return movimientos.isEmpty() && balance.ventasNetas().signum() == 0;
        }
    }

    private final VentaRepository ventas;
    private final DevolucionRepository devoluciones;
    private final MovimientoFinancieroRepository movimientos;

    public ReporteService(VentaRepository ventas, DevolucionRepository devoluciones,
                          MovimientoFinancieroRepository movimientos) {
        this.ventas = ventas;
        this.devoluciones = devoluciones;
        this.movimientos = movimientos;
    }

    /** Valida el periodo: la fecha inicial no puede ser posterior a la final. */
    public static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ReglaDeNegocioException("La fecha inicial no puede ser posterior a la final");
        }
    }

    @Transactional(readOnly = true)
    public ReporteVentas reporteVentas(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        List<Venta> registradas = ventas.entre(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()).stream()
                .filter(v -> v.getEstado() == EstadoVenta.REGISTRADA).toList();
        Map<Integer, BigDecimal> devuelto = devueltoPorVenta(registradas);

        BigDecimal bruto = BigDecimal.ZERO;
        BigDecimal descontado = BigDecimal.ZERO;
        List<FilaVenta> filas = new java.util.ArrayList<>();
        for (Venta v : registradas) {
            BigDecimal dev = devuelto.getOrDefault(v.getIdVenta(), BigDecimal.ZERO);
            filas.add(new FilaVenta(v.getIdVenta(), v.getFechaHora().format(FECHA_HORA),
                    v.getUsuario().getNombreCompleto(), v.getTotal(), dev, v.getTotal().subtract(dev)));
            bruto = bruto.add(v.getTotal());
            descontado = descontado.add(dev);
        }
        return new ReporteVentas(desde, hasta, filas, bruto, descontado, bruto.subtract(descontado));
    }

    /** RF-14: balance del periodo. */
    @Transactional(readOnly = true)
    public Balance balance(LocalDate desde, LocalDate hasta) {
        return reporteFinanciero(desde, hasta).balance();
    }

    @Transactional(readOnly = true)
    public ReporteFinanciero reporteFinanciero(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        BigDecimal ventasNetas = reporteVentas(desde, hasta).totalNeto();
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal egresos = BigDecimal.ZERO;
        List<FilaMovimiento> filas = new java.util.ArrayList<>();
        for (MovimientoFinanciero m : movimientos.entre(desde, hasta)) {
            boolean esIngreso = m.getTipo() == TipoMovimiento.INGRESO;
            if (esIngreso) {
                ingresos = ingresos.add(m.getValor());
            } else {
                egresos = egresos.add(m.getValor());
            }
            filas.add(new FilaMovimiento(m.getFecha().format(FECHA), esIngreso ? "Ingreso" : "Egreso",
                    m.getConcepto(), m.getProveedor() == null ? "" : m.getProveedor().getNombre(), m.getValor()));
        }
        Balance balance = new Balance(desde, hasta, ventasNetas, ingresos, egresos,
                ventasNetas.add(ingresos).subtract(egresos));
        return new ReporteFinanciero(balance, filas);
    }

    // ---- CSV compatible con Excel en español: UTF-8 con BOM, separador ";" y coma decimal.

    public String csvVentas(ReporteVentas r) {
        StringBuilder csv = new StringBuilder("﻿");
        fila(csv, "Reporte de ventas", r.desde().format(FECHA), r.hasta().format(FECHA));
        fila(csv, "Venta", "Fecha y hora", "Atendió", "Total", "Devuelto", "Neto");
        for (FilaVenta f : r.filas()) {
            fila(csv, "V-" + f.idVenta(), f.fechaHora(), f.atendio(), num(f.total()), num(f.devuelto()), num(f.neto()));
        }
        fila(csv, "Total", "", "", num(r.totalBruto()), num(r.totalDevuelto()), num(r.totalNeto()));
        return csv.toString();
    }

    public String csvFinanciero(ReporteFinanciero r) {
        StringBuilder csv = new StringBuilder("﻿");
        Balance b = r.balance();
        fila(csv, "Reporte financiero", b.desde().format(FECHA), b.hasta().format(FECHA));
        fila(csv, "Fecha", "Tipo", "Concepto", "Proveedor", "Valor");
        for (FilaMovimiento f : r.movimientos()) {
            fila(csv, f.fecha(), f.tipo(), f.concepto(), f.proveedor(), num(f.valor()));
        }
        fila(csv, "");
        fila(csv, "Ventas netas", "", "", "", num(b.ventasNetas()));
        fila(csv, "Otros ingresos", "", "", "", num(b.otrosIngresos()));
        fila(csv, "Egresos", "", "", "", num(b.egresos()));
        fila(csv, "Balance", "", "", "", num(b.balance()));
        return csv.toString();
    }

    private Map<Integer, BigDecimal> devueltoPorVenta(List<Venta> lista) {
        Map<Integer, BigDecimal> mapa = new HashMap<>();
        if (lista.isEmpty()) {
            return mapa;
        }
        for (Object[] fila : devoluciones.valorDevueltoPorVenta(lista.stream().map(Venta::getIdVenta).toList())) {
            mapa.put((Integer) fila[0], (BigDecimal) fila[1]);
        }
        return mapa;
    }

    private static String num(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static void fila(StringBuilder csv, String... celdas) {
        for (int i = 0; i < celdas.length; i++) {
            if (i > 0) {
                csv.append(';');
            }
            csv.append(celda(celdas[i]));
        }
        csv.append("\r\n");
    }

    /** Escapa comillas y separadores, y neutraliza celdas que Excel interpretaría como fórmula. */
    static String celda(String texto) {
        String valor = texto == null ? "" : texto;
        if (!valor.isEmpty() && "=+-@\t\r".indexOf(valor.charAt(0)) >= 0 && !valor.matches("-?[0-9]+(,[0-9]+)?")) {
            valor = "'" + valor;
        }
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            valor = "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
