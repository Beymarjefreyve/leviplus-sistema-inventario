package com.leviplus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * En producción las tablas las crea db/esquema.sql y Hibernate solo las valida (ddl-auto=validate). Esta prueba
 * arranca la aplicación así: si una entidad no coincide con el esquema, el contexto no levanta.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:validacion;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/esquema.sql"
})
@ActiveProfiles("test")
class EntidadesContraEsquemaTest {

    @Test
    void lasEntidadesCoincidenConElEsquemaSql() {
        // Si el contexto arranca, Hibernate validó todas las entidades contra las tablas de esquema.sql.
    }
}
