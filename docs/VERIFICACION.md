# Comprobaciones de esta entrega

## Backend

`mvn verify`: **9 pruebas de integración, sin errores ni fallos**, utilizando H2 en modo compatible con MySQL.

- Registro, inicio de sesión y hash BCrypt.
- Acceso anónimo bloqueado y solicitudes sin CSRF rechazadas.
- Aislamiento entre cuentas al leer, actualizar, eliminar y asociar categorías.
- Creación, edición y eliminación de movimientos; conservación de decimales.
- Rechazo de montos negativos, exceso de decimales y fechas futuras.
- Presupuestos únicos por mes y privacidad de presupuestos y metas.
- Rechazo de correos duplicados y categorías en uso.
- Generación real de los tres PDF y reporte de un mes vacío.
- Autorización del administrador, desactivación, reactivación y protección de cuentas administradoras.

Se ejecutó además el JAR mediante HTTP real: registro, cookies de sesión, CSRF, dos cuentas separadas, carga de ejemplos, tres descargas PDF, cierre de sesión y reinicio del servidor. Tras reiniciar, los 36 movimientos de prueba seguían guardados en la base H2 de disco.

## Interfaz y PDF

Se revisaron el panel de escritorio, las gráficas, la búsqueda de movimientos, el formulario y una vista de 390 px de ancho. En la vista móvil comprobada no hubo desbordamiento horizontal del documento. La inspección visual utilizó datos ficticios exportados por el backend de pruebas; no sustituye una prueba manual en todos los modelos de celular.

Los tres PDF generados por JasperReports se renderizaron e inspeccionaron: texto legible, columnas alineadas, importes y numeración de página presentes.

## Alcance pendiente

Los perfiles y el esquema de MySQL/MariaDB están incluidos, pero no se probaron contra servidores reales de esos motores en este entorno. También quedan por verificar Windows/macOS, el contenedor Docker y un despliegue público con HTTPS. El ejecutable se comprobó en Linux con Java 25.

Antes de abrir el registro al público, completa la recuperación de cuentas, verificación de correo, control de intentos, supervisión, copias de seguridad y revisión de dependencias. Esta entrega es una primera versión funcional para uso local y aprendizaje.
