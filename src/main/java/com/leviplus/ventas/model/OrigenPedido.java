package com.leviplus.ventas.model;

/** Medio por el que llegó el pedido. WhatsApp solo identifica el origen: no hay integración con el servicio. */
public enum OrigenPedido {

    WHATSAPP("whatsapp", "WhatsApp"),
    LLAMADA("llamada", "Llamada"),
    PRESENCIAL("presencial", "Presencial");

    private final String valorBd;
    private final String etiqueta;

    OrigenPedido(String valorBd, String etiqueta) {
        this.valorBd = valorBd;
        this.etiqueta = etiqueta;
    }

    public String getValorBd() {
        return valorBd;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static OrigenPedido deValorBd(String valor) {
        for (OrigenPedido origen : values()) {
            if (origen.valorBd.equals(valor)) {
                return origen;
            }
        }
        throw new IllegalArgumentException("Origen de pedido desconocido: " + valor);
    }
}
