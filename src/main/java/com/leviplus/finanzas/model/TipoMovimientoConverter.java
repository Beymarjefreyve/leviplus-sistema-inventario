package com.leviplus.finanzas.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoMovimientoConverter implements AttributeConverter<TipoMovimiento, String> {

    @Override
    public String convertToDatabaseColumn(TipoMovimiento tipo) {
        return tipo == null ? null : tipo.getValorBd();
    }

    @Override
    public TipoMovimiento convertToEntityAttribute(String valor) {
        return valor == null ? null : TipoMovimiento.deValorBd(valor);
    }
}
