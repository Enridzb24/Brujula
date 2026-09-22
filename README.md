# Brújula

Una aplicación web para registrar ingresos y gastos, revisar el mes y seguir metas personales. Se usa desde el navegador en computadora o celular.

## Probar sin configurar MySQL

1. Descomprime todo el ZIP en una carpeta propia.
2. Comprueba que tienes Java 25 o superior: `java -version`.
3. En Windows, abre `iniciar-windows.bat`. En macOS o Linux, ejecuta `sh iniciar.sh`.
4. Espera el mensaje `Started BrujulaApplication`.
5. Abre **http://localhost:8091** en tu navegador.
6. Crea una cuenta. No se incluyen usuarios ni contraseñas predeterminadas.
7. Puedes registrar tus propios movimientos o pulsar **Explorar con ejemplos** en una cuenta vacía.

La aplicación no necesita conexión a Internet una vez que tienes Java y el ZIP: el código de las gráficas está incluido. Al cerrar la ventana del servidor, la web deja de responder. Los datos permanecen en `data/brujula.mv.db`; no borres esa carpeta si quieres conservarlos. Para hacer una copia de seguridad de H2, detén primero la aplicación y copia `data/` completa.

El modo rápido utiliza **H2**, una base SQL persistente incluida en el ejecutable. No simula usuarios ni guarda tus movimientos en el navegador. El mismo backend puede conectarse a MySQL o MariaDB mediante sus perfiles. Cambiar de motor no migra los datos existentes.

## Abrir en VS Code y modificar

Abre la carpeta que contiene `pom.xml`. Necesitas un JDK 25 o superior. Las extensiones de Java de VS Code son opcionales pero facilitan ejecutar y depurar.

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```sh
./mvnw spring-boot:run
```

Para ejecutar las pruebas y generar un nuevo ejecutable:

```powershell
.\mvnw.cmd clean verify
```

El resultado queda en `target/brujula-1.0.0.jar`. El `brujula.jar` de la raíz es una copia precompilada: para usar tus cambios, ejecuta el JAR de `target/` o reemplaza esa copia. Maven necesita Internet en la primera compilación para descargar sus dependencias.

## Usar MySQL o XAMPP

Consulta `docs/BASE_DE_DATOS.md`. Incluye creación de tablas, relaciones, consultas de práctica y configuración del usuario de conexión.

phpMyAdmin es la herramienta de administración, no el motor. Muchas instalaciones de XAMPP utilizan MariaDB aunque el panel diga MySQL. Por eso están incluidos ambos controladores y perfiles; elige el que corresponda a tu servidor.

## Verlo desde un celular

1. Conecta el celular y la computadora a la misma red Wi-Fi de confianza.
2. Mantén Brújula ejecutándose en la computadora.
3. En Windows, usa `ipconfig` para localizar la dirección IPv4 de tu conexión Wi-Fi.
4. En el celular, abre `http://TU_IP_LOCAL:8091`, sustituyendo `TU_IP_LOCAL` por esa dirección, por ejemplo `192.168.1.20`.
5. Si Windows lo solicita, permite Java solo en la red privada. No necesitas abrir puertos en el router.

El celular consulta el mismo servidor y la misma base. **localhost en el celular apunta al celular, no a tu computadora.** El modo local usa HTTP y está pensado para pruebas en una red de confianza; un servicio público debe usar HTTPS.

## Qué incluye

- Registro de cuentas e inicio y cierre de sesión.
- Panel de administración: listado de cuentas, activación y desactivación, sin acceso a las finanzas ajenas.
- Contraseñas con BCrypt, sesiones HTTP y protección CSRF.
- Separación de datos por usuario en consultas y relaciones SQL.
- Crear, editar y eliminar ingresos y gastos.
- Categorías propias de ingresos y gastos; eliminación protegida si están en uso.
- Historial por mes, búsqueda, filtros y paginación.
- Resumen de saldo acumulado, ingresos, gastos y balance mensual.
- Gráficas de seis meses y distribución de gastos.
- Presupuestos por categoría y mes, con avisos visuales al acercarse o superar el límite.
- Metas con importe objetivo, ahorro reservado y fecha.
- Tres PDF con JasperReports: movimientos, gastos por categoría y resumen financiero.
- Interfaz responsive, tema claro y oscuro.
- Ejemplos opcionales en una cuenta vacía, sin credenciales compartidas.
- SQL, código fuente, plantilla JRXML y pruebas de integración.

## Cómo se interpretan los números

Para habilitar tu cuenta administradora, sigue `docs/ADMINISTRADOR.md`. El rol se asigna desde el equipo que ejecuta la aplicación, no desde el registro público.

| Dato | Cálculo |
| --- | --- |
| Ingresos del mes | Suma de ingresos del mes seleccionado |
| Gastos del mes | Suma de gastos del mes seleccionado |
| Balance del mes | Ingresos del mes menos gastos del mes |
| Saldo acumulado | Todos los ingresos registrados menos todos los gastos registrados |
| Ahorro reservado en metas | Cantidad que tú indicas manualmente en cada meta |

El saldo acumulado parte de cero. Si necesitas reflejar dinero que ya tenías, registra un ingreso inicial y categorízalo según tu criterio. Las metas son referencias de planificación: no apartan dinero bancario, no cambian el saldo y no generan movimientos. La aplicación no se conecta con bancos ni realiza pagos. Los montos se guardan como `DECIMAL(12,2)` y Java los maneja con `BigDecimal`.

## Estructura para aprender

```text
src/main/java/pe/brujula/
  config/        Seguridad y autenticación
  controller/    Rutas HTTP y errores
  service/       Reglas del sistema y PDF
  repository/    Consultas SQL parametrizadas
  model/         Datos y validaciones
src/main/resources/
  static/        HTML, CSS y JavaScript
  reports/       Plantilla JasperReports
  schema.sql     Tablas del modo rápido
src/test/        Pruebas de integración
sql/             Script para MySQL y MariaDB
docs/            Guías de base de datos y aprendizaje
```

El frontend usa HTML, CSS propio y JavaScript con Chart.js. Se comunica con controladores REST de Spring Boot. Se eligió JDBC para dejar las consultas SQL visibles y practicar lo aprendido en base de datos. Esta edición no utiliza Thymeleaf, JPA ni Bootstrap: la interfaz y el backend están separados y el CSS es propio.

## Publicación en Internet

El ZIP contiene una aplicación ejecutable, **no un servicio público ya alojado**. Para tener una URL permanente hace falta un servidor compatible con Java o contenedores y una base MySQL/MariaDB accesible desde ese servidor. El `Dockerfile` permite construir una imagen; no se ha desplegado ni probado ese contenedor en un proveedor.

En producción configura `SPRING_PROFILES_ACTIVE=mysql,prod` (o `mariadb,prod`), `DB_URL`, `DB_USER` y `DB_PASSWORD`, y HTTPS mediante el proveedor o un proxy de confianza. El perfil `prod` exige cookies seguras. No subas credenciales a Git. Haz copias de seguridad, define límites de peticiones y revisa las dependencias antes de abrir el registro al público.

Esta primera versión no incluye verificación del correo, recuperación de contraseña por email, doble factor, control de intentos, conexión bancaria ni despliegue público. Los registros se eliminan definitivamente después de confirmar en la interfaz. No está presentada como un servicio financiero listo para operar a gran escala.

## Referencias técnicas

- Spring Boot: https://docs.spring.io/spring-boot/3.5/
- Spring Security: https://docs.spring.io/spring-security/reference/
- JasperReports 6.21.3: https://jasperreports.sourceforge.net/6.21.3/api/
- Chart.js: https://www.chartjs.org/docs/4.4.8/

El proyecto fija sus versiones en `pom.xml`; la plantilla JRXML corresponde a JasperReports 6.21.3 y debe editarse con compatibilidad para esa versión.
