package com.leviplus.ventas.model;

/** Qué se hace con el producto devuelto: volver al inventario o darse de baja. */
public enum DestinoDevolucion {

    REINTEGRO("reintegro", "Reintegrar al inventario"),
    BAJA("baja", "Dar de baja");

    private final String valorBd;
    private final String etiqueta;

    DestinoDevolucion(String valorBd, String etiqueta) {
        this.valorBd = valorBd;
        this.etiqueta = etiqueta;
    }

    public String getValorBd() {
        return valorBd;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static DestinoDevolucion deValorBd(String valor) {
        for (DestinoDevolucion destino : values()) {
            if (destino.valorBd.equals(valor)) {
                return destino;
            }
        }
        throw new IllegalArgumentException("Destino de devolución desconocido: " + valor);
    }
}
