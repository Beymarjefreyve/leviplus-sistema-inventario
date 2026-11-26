package com.leviplus.finanzas.service;

import com.leviplus.acceso.model.Usuario;
import com.leviplus.acceso.repository.UsuarioRepository;
import com.leviplus.common.ReglaDeNegocioException;
import com.leviplus.finanzas.controller.MovimientoForm;
import com.leviplus.finanzas.model.MovimientoFinanciero;
import com.leviplus.finanzas.model.TipoMovimiento;
import com.leviplus.finanzas.repository.MovimientoFinancieroRepository;
import com.leviplus.inventario.model.Proveedor;
import com.leviplus.inventario.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MovimientoService {

    private final MovimientoFinancieroRepository movimientos;
    private final ProveedorRepository proveedores;
    private final UsuarioRepository usuarios;

    public MovimientoService(MovimientoFinancieroRepository movimientos, ProveedorRepository proveedores,
                             UsuarioRepository usuarios) {
        this.movimientos = movimientos;
        this.proveedores = proveedores;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<MovimientoFinanciero> listar() {
        return movimientos.findAllConDetalle();
    }

    /** RF-13: otros ingresos distintos de la venta de productos. */
    @Transactional
    public MovimientoFinanciero registrarIngreso(MovimientoForm datos, String nombreUsuario) {
        String concepto = datos.getConcepto() == null ? "" : datos.getConcepto().trim();
        if (concepto.isEmpty()) {
            throw new ReglaDeNegocioException("El concepto del ingreso es obligatorio");
        }
        validarFecha(datos.getFecha());
        return movimientos.save(new MovimientoFinanciero(TipoMovimiento.INGRESO, concepto, null,
                datos.getFecha(), datos.getValor(), usuario(nombreUsuario)));
    }

    /**
     * RF-12: egreso por pago a un proveedor registrado o por otro gasto operativo.
     * Debe indicarse el proveedor, el concepto o ambos.
     */
    @Transactional
    public MovimientoFinanciero registrarEgreso(MovimientoForm datos, String nombreUsuario) {
        String concepto = datos.getConcepto() == null ? "" : datos.getConcepto().trim();
        Proveedor proveedor = null;
        if (datos.getIdProveedor() != null) {
            proveedor = proveedores.findById(datos.getIdProveedor())
                    .orElseThrow(() -> new ReglaDeNegocioException("El proveedor no existe"));
            if (!proveedor.estaActivo()) {
                throw new ReglaDeNegocioException("El proveedor está inactivo");
            }
            if (concepto.isEmpty()) {
                concepto = "Pago a proveedor: " + proveedor.getNombre();
            }
        }
        if (concepto.isEmpty()) {
            throw new ReglaDeNegocioException("Selecciona un proveedor o escribe el concepto del gasto");
        }
        validarFecha(datos.getFecha());
        return movimientos.save(new MovimientoFinanciero(TipoMovimiento.EGRESO, concepto, proveedor,
                datos.getFecha(), datos.getValor(), usuario(nombreUsuario)));
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            throw new ReglaDeNegocioException("La fecha no puede ser posterior a hoy");
        }
    }

    private Usuario usuario(String nombreUsuario) {
        return usuarios.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ReglaDeNegocioException("Usuario no válido"));
    }
}
