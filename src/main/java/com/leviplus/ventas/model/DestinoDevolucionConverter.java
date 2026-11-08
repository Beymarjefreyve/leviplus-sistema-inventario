package com.leviplus.ventas.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DestinoDevolucionConverter implements AttributeConverter<DestinoDevolucion, String> {

    @Override
    public String convertToDatabaseColumn(DestinoDevolucion destino) {
        return destino == null ? null : destino.getValorBd();
    }

    @Override
    public DestinoDevolucion convertToEntityAttribute(String valor) {
        return valor == null ? null : DestinoDevolucion.deValorBd(valor);
    }
}
