package com.leviplus.consultas.controller;

import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.consultas.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Reportes de ventas y financieros por periodo, en pantalla y en CSV (RF-16). Solo el administrador. */
@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String reporte(@RequestParam(defaultValue = "ventas") String tipo,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                          Model modelo) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.withDayOfMonth(1) : desde;
        boolean financiero = "financiero".equals(tipo);
        modelo.addAttribute("tipo", financiero ? "financiero" : "ventas");
        modelo.addAttribute("desde", inicio);
        modelo.addAttribute("hasta", fin);
        try {
            if (financiero) {
                modelo.addAttribute("reporte", servicio.reporteFinanciero(inicio, fin));
            } else {
                modelo.addAttribute("reporte", servicio.reporteVentas(inicio, fin));
            }
        } catch (ReglaDeNegocioException e) {
            modelo.addAttribute("error", e.getMessage());
        }
        return "consultas/reportes";
    }

    @GetMapping("/csv")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> csv(@RequestParam String tipo,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        boolean financiero = "financiero".equals(tipo);
        String contenido = financiero
                ? servicio.csvFinanciero(servicio.reporteFinanciero(desde, hasta))
                : servicio.csvVentas(servicio.reporteVentas(desde, hasta));
        String nombre = "reporte-" + (financiero ? "financiero" : "ventas") + "-" + desde + "_" + hasta + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenido.getBytes(StandardCharsets.UTF_8));
    }
}
