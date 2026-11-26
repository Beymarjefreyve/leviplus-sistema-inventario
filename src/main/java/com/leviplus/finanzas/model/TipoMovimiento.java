package com.leviplus.finanzas.model;

public enum TipoMovimiento {

    INGRESO("ingreso"),
    EGRESO("egreso");

    private final String valorBd;

    TipoMovimiento(String valorBd) {
        this.valorBd = valorBd;
    }

    public String getValorBd() {
        return valorBd;
    }

    public static TipoMovimiento deValorBd(String valor) {
        for (TipoMovimiento tipo : values()) {
            if (tipo.valorBd.equals(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de movimiento desconocido: " + valor);
    }
}
