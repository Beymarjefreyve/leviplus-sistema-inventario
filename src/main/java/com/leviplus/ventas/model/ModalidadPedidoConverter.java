package com.leviplus.ventas.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ModalidadPedidoConverter implements AttributeConverter<ModalidadPedido, String> {

    @Override
    public String convertToDatabaseColumn(ModalidadPedido modalidad) {
        return modalidad == null ? null : modalidad.getValorBd();
    }

    @Override
    public ModalidadPedido convertToEntityAttribute(String valor) {
        return valor == null ? null : ModalidadPedido.deValorBd(valor);
    }
}
