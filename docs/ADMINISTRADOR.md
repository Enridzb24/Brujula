# Administrador

Las cuentas creadas desde la web siempre empiezan como usuarios normales. No hay un administrador con una contraseña compartida ni una opción pública para elegir ese rol.

## Convertir tu cuenta en administradora

1. Inicia Brújula y registra tu cuenta normalmente.
2. Detén el servidor con Ctrl+C.
3. Desde la misma carpeta y usando la misma base de datos, inicia una vez con tu correo:

```powershell
java -Duser.timezone=America/Lima -jar brujula.jar --brujula.admin-email=TU_CORREO
```

Sustituye `TU_CORREO` por el correo de la cuenta que acabas de crear. Si trabajas con MySQL o MariaDB, mantén sus variables y añade también `--spring.profiles.active=mysql` o `mariadb`.

4. Inicia sesión de nuevo. Aparecerá **Administración** en el menú.
5. En los siguientes arranques usa el inicio normal, sin `--brujula.admin-email`. El rol queda guardado en la base.

Este parámetro solo funciona para una cuenta existente. Si no existe, el servidor detiene el arranque con un mensaje. Se utiliza desde la computadora o servidor que controla la aplicación, nunca desde un formulario público.

También puedes asignar el rol desde tu administrador SQL:

```sql
UPDATE users SET role = 'ADMIN', enabled = TRUE
WHERE email = 'TU_CORREO';
```

Comprueba el correo exacto antes de ejecutar la consulta.

## Qué puede hacer

- Ver nombre, correo, rol y estado de las cuentas.
- Ver cuántas cuentas están activas o desactivadas.
- Desactivar usuarios sin borrar sus registros.
- Reactivar usuarios.
- Usar su propio panel de finanzas como cualquier usuario.

El panel no muestra gastos, ingresos, metas ni contraseñas de otros usuarios. No permite desactivar al propio administrador ni a otra cuenta administradora. Tampoco permite conceder roles desde la web.

Al desactivar una cuenta, sus siguientes solicitudes de datos se rechazan, aunque tuviera una sesión abierta. Las cifras que ya estaban cargadas en su pantalla pueden permanecer visibles hasta la siguiente acción o recarga. No se envía una notificación automática a otros dispositivos.

Quien tenga acceso directo al servidor o a la base de datos puede consultar las tablas: la privacidad descrita se refiere al control de acceso de la aplicación, no a cifrado de extremo a extremo.
