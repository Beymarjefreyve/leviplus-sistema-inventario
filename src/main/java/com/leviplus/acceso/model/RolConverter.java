package com.leviplus.acceso.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RolConverter implements AttributeConverter<Rol, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Rol rol) {
        return rol == null ? null : rol.getId();
    }

    @Override
    public Rol convertToEntityAttribute(Integer id) {
        return id == null ? null : Rol.deId(id);
    }
}
