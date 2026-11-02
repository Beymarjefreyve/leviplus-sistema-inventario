package com.leviplus.acceso.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** El modelo de datos guarda el estado en minusculas ('activo', 'inactivo'). */
@Converter(autoApply = true)
public class EstadoRegistroConverter implements AttributeConverter<EstadoRegistro, String> {

    @Override
    public String convertToDatabaseColumn(EstadoRegistro estado) {
        return estado == null ? null : estado.name().toLowerCase();
    }

    @Override
    public EstadoRegistro convertToEntityAttribute(String valor) {
        return valor == null ? null : EstadoRegistro.valueOf(valor.toUpperCase());
    }
}
