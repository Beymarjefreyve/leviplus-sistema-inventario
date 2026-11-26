package com.leviplus.inventario.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MotivoSalidaConverter implements AttributeConverter<MotivoSalida, String> {

    @Override
    public String convertToDatabaseColumn(MotivoSalida motivo) {
        return motivo == null ? null : motivo.getValorBd();
    }

    @Override
    public MotivoSalida convertToEntityAttribute(String valor) {
        return valor == null ? null : MotivoSalida.deValorBd(valor);
    }
}
