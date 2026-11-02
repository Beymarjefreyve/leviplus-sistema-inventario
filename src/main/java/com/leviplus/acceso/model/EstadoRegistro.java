package com.leviplus.acceso.model;

/**
 * Estado de usuarios, productos y proveedores. Inactivar reemplaza a eliminar,
 * para conservar el historial de operaciones.
 */
public enum EstadoRegistro {
    ACTIVO,
    INACTIVO
}
