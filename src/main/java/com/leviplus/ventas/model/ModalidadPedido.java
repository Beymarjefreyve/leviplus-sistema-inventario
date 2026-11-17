package com.leviplus.ventas.model;

/** Cómo se entrega el pedido: a domicilio o recogido en la droguería. */
public enum ModalidadPedido {

    DOMICILIO("domicilio", "A domicilio"),
    RECOGE("recoge", "Recoge en la droguería");

    private final String valorBd;
    private final String etiqueta;

    ModalidadPedido(String valorBd, String etiqueta) {
        this.valorBd = valorBd;
        this.etiqueta = etiqueta;
    }

    public String getValorBd() {
        return valorBd;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static ModalidadPedido deValorBd(String valor) {
        for (ModalidadPedido modalidad : values()) {
            if (modalidad.valorBd.equals(valor)) {
                return modalidad;
            }
        }
        throw new IllegalArgumentException("Modalidad de pedido desconocida: " + valor);
    }
}
