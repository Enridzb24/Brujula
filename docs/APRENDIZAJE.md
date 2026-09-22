# Reconstruir Brújula desde cero

Primero usa esta versión: crea una cuenta, carga ejemplos y recorre cada pantalla. Después conserva esta carpeta como referencia y abre un proyecto nuevo para practicar.

| Etapa | Construir | Entender | Comprobar |
| --- | --- | --- | --- |
| 1 | Requisitos y bocetos | Usuarios, casos de uso y reglas | Explicar qué puede hacer una cuenta |
| 2 | Tablas SQL | PK, FK, UNIQUE, CHECK y normalización | Insertar datos y hacer un JOIN |
| 3 | Proyecto Java | Clases, records, tipos y paquetes | Ejecutar la aplicación |
| 4 | Primer endpoint | HTTP, GET, POST y JSON | Obtener una respuesta en el navegador |
| 5 | Repository | SQL parametrizado y JDBC | Guardar y consultar un movimiento |
| 6 | Service | Reglas de negocio y transacciones | Rechazar un dato inválido |
| 7 | Registro y login | BCrypt, sesión, CSRF y autorización | Impedir acceso entre dos cuentas |
| 8 | Formularios | HTML, eventos y fetch | Guardar desde la interfaz |
| 9 | Diseño responsive | CSS Grid, Flexbox y media queries | Usar el formulario en celular |
| 10 | Resumen y gráficas | Filtrar por mes y sumar montos | Comparar la gráfica con SQL |
| 11 | Presupuestos y metas | Reglas y significado de cada cifra | Superar un presupuesto de prueba |
| 12 | PDF | JRXML, compilación, llenado y exportación | Descargar un reporte propio |
| 13 | Pruebas y versiones | Pruebas de integración y Git | Guardar avances pequeños y verificables |

El recorrido principal es: formulario → controlador → servicio → repositorio → base de datos. La respuesta vuelve al navegador y actualiza las cifras.

## Primer ejercicio al reconstruir

Crea solamente una tabla `movimiento`, con descripción, monto y fecha. Inserta tres filas manualmente. Consulta cuánto gastaste en total. Luego implementa ese mismo recorrido desde Java y finalmente desde un formulario. Añade usuarios después de entender el flujo; no publiques esa etapa sin control de acceso.

## Cómo trabajar cada sesión

Define un cambio pequeño, escribe una parte, ejecútala y explica con tus palabras qué pasó. Guarda el avance con Git. Si aparece un error, lee primero su mensaje y localiza la capa donde ocurre antes de cambiar código.

Preguntas útiles: ¿de dónde llega este dato?, ¿quién puede modificarlo?, ¿qué pasa si está vacío?, ¿qué consulta se ejecuta?, ¿cómo sé que quedó guardado?
