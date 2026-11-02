# Levi Plus · Sistema de inventario

Sistema web para el control de inventario y la gestión operativa de la **Droguería Levi Plus**.
Proyecto académico de la Universidad Francisco de Paula Santander (UFPS), equipo Next Gen Tech Solutions.

El sistema controla productos y proveedores, entradas y salidas de inventario **por lote**, ventas con descuento
automático del lote que vence primero, anulaciones y devoluciones, clientes frecuentes y pedidos, movimientos
financieros y balance, y alertas de vencimiento y bajo stock con reportes exportables a CSV.

## Tecnología

| Capa | Tecnología |
|---|---|
| Lenguaje y framework | Java 21, Spring Boot 3.5 (Spring MVC) |
| Vistas | Thymeleaf |
| Seguridad | Spring Security (sesión, roles, CSRF, contraseñas BCrypt) |
| Persistencia | Spring Data JPA, PostgreSQL |
| Despliegue | Docker en Google Cloud Run, base de datos en Neon |

## Estructura

```
src/main/java/com/leviplus/
  config/        seguridad y configuración general
  common/        utilidades compartidas
  acceso/        módulo 1: acceso y usuarios
  inventario/    módulo 2: productos, proveedores, lotes, entradas y salidas
  ventas/        módulo 3: ventas, devoluciones, clientes frecuentes y pedidos
  finanzas/      módulo 4: ingresos, egresos y balance
  consultas/     módulo 5: panel, alertas, existencias y reportes
    (cada módulo: controller/, service/, repository/, model/)
src/main/resources/
  templates/     vistas Thymeleaf        static/   css, js, imágenes
  db/esquema.sql script de las 16 tablas del modelo lógico
```

## Ejecutar en local

Requisitos: JDK 21 o superior y Docker.

```bash
# 1. Variables de entorno (el archivo .env no se sube al repositorio)
cp .env.example .env          # en Windows: copy .env.example .env
#    edita .env: define DB_PASSWORD y la contraseña del administrador inicial

# 2. Base de datos local (crea las tablas con db/esquema.sql)
docker compose up -d db

# 3. Aplicación
./mvnw spring-boot:run        # en Windows: mvnw.cmd spring-boot:run
```

Abre <http://localhost:8080>. Al primer arranque, si no hay usuarios, se crea el administrador con
`LEVIPLUS_ADMIN_USER` y `LEVIPLUS_ADMIN_PASSWORD` del `.env`.

Pruebas: `./mvnw test` (usan una base en memoria; no necesitan Docker).

Para reiniciar la base local desde cero: `docker compose down -v` y luego `docker compose up -d db`.

## Despliegue

La imagen se construye con el `Dockerfile` y se publica en Google Cloud Run. La conexión a Neon y las
credenciales se entregan **solo como variables de entorno** (`DB_URL`, `DB_USER`, `DB_PASSWORD`,
`LEVIPLUS_ADMIN_USER`, `LEVIPLUS_ADMIN_PASSWORD`). Las tablas se crean una vez ejecutando
`src/main/resources/db/esquema.sql` en el editor SQL de Neon.

## Seguridad del repositorio

Este repositorio es público. **Nunca** subas contraseñas, cadenas de conexión ni datos reales de la droguería
(clientes, teléfonos, inventario). Consulta [CONTRIBUTING.md](CONTRIBUTING.md).
