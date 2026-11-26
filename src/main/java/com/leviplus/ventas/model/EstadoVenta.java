package com.leviplus.ventas.model;

public enum EstadoVenta {

    REGISTRADA("registrada"),
    ANULADA("anulada");

    private final String valorBd;

    EstadoVenta(String valorBd) {
        this.valorBd = valorBd;
    }

    public String getValorBd() {
        return valorBd;
    }

    public static EstadoVenta deValorBd(String valor) {
        for (EstadoVenta estado : values()) {
            if (estado.valorBd.equals(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de venta desconocido: " + valor);
    }
}
