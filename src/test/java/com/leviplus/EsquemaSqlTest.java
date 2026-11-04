package com.leviplus;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EsquemaSqlTest {

    private static final Set<String> TABLAS = Set.of(
            "ROL", "USUARIO", "PRODUCTO", "PROVEEDOR", "LOTE", "ENTRADA_INVENTARIO", "SALIDA_INVENTARIO",
            "VENTA", "DETALLE_VENTA", "DETALLE_VENTA_LOTE", "DEVOLUCION", "DETALLE_DEVOLUCION",
            "CLIENTE_FRECUENTE", "PEDIDO", "DETALLE_PEDIDO", "MOVIMIENTO_FINANCIERO");

    @Test
    void creaLasDieciseisTablas() throws Exception {
        try (Connection con = DriverManager.getConnection("jdbc:h2:mem:esquema;MODE=PostgreSQL", "sa", "")) {
            ScriptUtils.executeSqlScript(con, new ClassPathResource("db/esquema.sql"));

            Set<String> creadas = new HashSet<>();
            try (ResultSet rs = con.getMetaData().getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    creadas.add(rs.getString("TABLE_NAME").toUpperCase());
                }
            }
            assertEquals(TABLAS, creadas);

            try (ResultSet rs = con.createStatement().executeQuery("SELECT COUNT(*) FROM rol")) {
                rs.next();
                assertEquals(2, rs.getInt(1), "ROL debe traer Administrador y Encargado de ventas");
            }
            assertTrue(creadas.size() == 16);
        }
    }
}
