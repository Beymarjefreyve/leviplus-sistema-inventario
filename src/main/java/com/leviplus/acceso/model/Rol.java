package com.leviplus.acceso.model;

/**
 * Roles del sistema (tabla ROL). Se modelan como enumeracion y no como herencia de Usuario:
 * solo cambian los permisos segun la matriz de permisos del documento ingenieril (seccion 1.3).
 */
public enum Rol {

    ADMINISTRADOR(1, "Administrador"),
    ENCARGADO_DE_VENTAS(2, "Encargado de ventas");

    private final int id;
    private final String nombre;

    Rol(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    /** Identificador en la tabla ROL (columna id_rol). */
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    /** Nombre de la autoridad en Spring Security, por ejemplo ROLE_ADMINISTRADOR. */
    public String autoridad() {
        return "ROLE_" + name();
    }

    public static Rol deId(int id) {
        for (Rol rol : values()) {
            if (rol.id == id) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol desconocido: " + id);
    }
}
