package com.leviplus.ventas.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrigenPedidoConverter implements AttributeConverter<OrigenPedido, String> {

    @Override
    public String convertToDatabaseColumn(OrigenPedido origen) {
        return origen == null ? null : origen.getValorBd();
    }

    @Override
    public OrigenPedido convertToEntityAttribute(String valor) {
        return valor == null ? null : OrigenPedido.deValorBd(valor);
    }
}
