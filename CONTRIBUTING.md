# Guía de colaboración

## Reglas de oro (el repositorio es público)

- **Nada de secretos en git:** contraseñas, cadenas de conexión de Neon, claves de sesión. Todo va en el archivo
  `.env` (ignorado) o en variables de entorno de Cloud Run.
- **Nada de datos reales** de la droguería en scripts, pruebas, capturas o README. Usa datos ficticios.
- Si una clave se sube por error, **cámbiala de inmediato** (en Neon se regenera desde el panel). Borrar el
  archivo no basta: queda en el historial.

## Flujo de trabajo

1. Actualiza `develop`: `git pull origin develop`.
2. Crea tu rama desde `develop` con el nombre de la tarea del cronograma:
   `feature/<modulo>-<tarea>`, por ejemplo `feature/inventario-productos`.
3. Haz commits pequeños y en español, con el formato `tipo(modulo): descripción`:
   - `feat(inventario): registrar entrada de producto por lote`
   - `fix(ventas): validar existencia antes de confirmar`
   - `test(acceso): probar usuario inactivo`
   - `docs`, `refactor`, `chore` según corresponda.
4. Antes de subir, ejecuta `./mvnw test`. Debe pasar todo.
5. Abre un Pull Request hacia `develop` y pide revisión a un compañero. `main` solo recibe la versión
   estable de cada iteración.

## Convenciones de código

- Un paquete por módulo, con `controller`, `service`, `repository` y `model`.
- Las reglas de negocio van **solo en los servicios**; los controladores validan la entrada y llaman al servicio;
  el acceso a datos va **solo en los repositorios**.
- Los métodos que escriben en varias tablas (venta, anulación, devolución) llevan `@Transactional`.
- Cada método de controlador lleva `@PreAuthorize` según la matriz de permisos del documento ingenieril
  (`hasRole("ADMINISTRADOR")` o `hasAnyRole("ADMINISTRADOR", "ENCARGADO_DE_VENTAS")`). Ocultar un botón en la
  vista no reemplaza esta validación.
- Consultas siempre parametrizadas (Spring Data JPA); nunca concatenar texto del usuario.
- Los nombres de tablas y clases siguen el modelo lógico y el diagrama de clases del documento ingenieril.
- Cada funcionalidad nueva lleva sus pruebas.
