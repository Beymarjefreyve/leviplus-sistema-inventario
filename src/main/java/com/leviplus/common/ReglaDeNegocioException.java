package com.leviplus.common;

/** Error de regla de negocio cuyo mensaje se muestra tal cual al usuario. */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
