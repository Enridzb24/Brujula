# Base de datos

## Tablas y relaciones

| Tabla | Propósito | Relaciones |
| --- | --- | --- |
| users | Cuentas y hash de contraseña | Un usuario tiene muchas categorías, movimientos, metas y presupuestos |
| categories | Categorías privadas y tipo de movimiento | Pertenece a un usuario |
| movements | Monto, fecha, descripción y medio de pago | Pertenece a un usuario y una categoría del mismo usuario |
| budgets | Límite por categoría y mes | Un presupuesto por usuario, categoría y mes |
| goals | Objetivo de ahorro y monto reservado | Pertenece a un usuario |

La clave foránea compuesta `(category_id, user_id)` evita asociar un movimiento o presupuesto con una categoría de otra persona. El tipo se obtiene de la categoría: no se duplica en `movements`. Las consultas de actualización y eliminación también llevan `user_id`; conocer un ID ajeno no permite modificarlo.

## Configurar MySQL o MariaDB

1. Inicia el servidor desde XAMPP o tu instalación de MySQL/MariaDB.
2. Abre phpMyAdmin, Workbench o el cliente SQL que corresponda.
3. Ejecuta `sql/01_esquema_mysql.sql` una sola vez. Crea `brujula_db` y sus tablas; no elimina bases ni registros previos.
4. Crea un usuario de conexión dedicado. Como administrador, sustituye la contraseña antes de ejecutar:

```sql
CREATE USER 'brujula_app'@'localhost' IDENTIFIED BY 'CAMBIA_ESTA_CLAVE';
GRANT SELECT, INSERT, UPDATE, DELETE ON brujula_db.* TO 'brujula_app'@'localhost';
```

5. En una terminal PowerShell abierta en el proyecto:

```powershell
$env:DB_USER = "brujula_app"
$env:DB_PASSWORD = "LA_CLAVE_QUE_ELEGISTE"
java -Duser.timezone=America/Lima -jar brujula.jar --spring.profiles.active=mysql
```

Si tu XAMPP utiliza MariaDB, cambia el último argumento por `--spring.profiles.active=mariadb`.

Los perfiles asumen el puerto 3306. Para otro servidor o puerto, configura `DB_URL` antes de iniciar. Ejemplo MySQL:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3307/brujula_db?serverTimezone=America/Lima"
```

No guardes contraseñas en los archivos del proyecto ni las subas a GitHub. El modo H2, MySQL y MariaDB utiliza bases independientes; al cambiar, crea tu cuenta en la base elegida. No hay migración automática del historial entre motores.

## Consultas para practicar

Sustituye el usuario y las fechas por valores que existan en tu base.

```sql
SELECT id, name, email FROM users;

SELECT m.movement_date, m.description, c.name AS category, c.kind, m.amount
FROM movements m
JOIN categories c ON c.id = m.category_id AND c.user_id = m.user_id
WHERE m.user_id = 1
  AND m.movement_date >= '2026-09-01'
  AND m.movement_date < '2026-10-01'
ORDER BY m.movement_date DESC;

SELECT c.name, SUM(m.amount) AS total_spent
FROM movements m
JOIN categories c ON c.id = m.category_id AND c.user_id = m.user_id
WHERE m.user_id = 1 AND c.kind = 'GASTO'
  AND m.movement_date >= '2026-09-01'
  AND m.movement_date < '2026-10-01'
GROUP BY c.id, c.name
ORDER BY total_spent DESC;
```

## Decisiones del modelo

- `DECIMAL`, no `FLOAT`, para dinero.
- `UNIQUE` en el correo y en el presupuesto mensual por categoría.
- `CHECK` para montos y tipos válidos; el backend también valida.
- Claves foráneas para mantener las relaciones.
- Índice por usuario y fecha en los movimientos.
- Contraseñas transformadas con BCrypt: no se guardan contraseñas legibles.
- No se utiliza `ddl-auto=update`; el esquema es explícito.

Para un despliegue de larga duración, los cambios futuros del esquema deben convertirse en migraciones versionadas. El script inicial no actualiza automáticamente tablas existentes.
