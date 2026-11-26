package com.leviplus.ventas.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoVentaConverter implements AttributeConverter<EstadoVenta, String> {

    @Override
    public String convertToDatabaseColumn(EstadoVenta estado) {
        return estado == null ? null : estado.getValorBd();
    }

    @Override
    public EstadoVenta convertToEntityAttribute(String valor) {
        return valor == null ? null : EstadoVenta.deValorBd(valor);
    }
}
