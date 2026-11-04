package com.leviplus.acceso.model;

public enum Rol {

    ADMINISTRADOR(1, "Administrador"),
    ENCARGADO_DE_VENTAS(2, "Encargado de ventas");

    private final int id;
    private final String nombre;

    Rol(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

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
