package com.leviplus.inventario.model;

public enum MotivoSalida {

    VENCIMIENTO("vencimiento", "Vencimiento"),
    DANO("dano", "Daño"),
    OTRO("otro", "Otro");

    private final String valorBd;
    private final String etiqueta;

    MotivoSalida(String valorBd, String etiqueta) {
        this.valorBd = valorBd;
        this.etiqueta = etiqueta;
    }

    public String getValorBd() {
        return valorBd;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static MotivoSalida deValorBd(String valor) {
        for (MotivoSalida motivo : values()) {
            if (motivo.valorBd.equals(valor)) {
                return motivo;
            }
        }
        throw new IllegalArgumentException("Motivo de salida desconocido: " + valor);
    }
}
