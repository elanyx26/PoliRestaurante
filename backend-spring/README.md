# PoliRestaurante API (Spring Boot)

Backend del restaurante: autenticación JWT, menú, pedidos con flujo por roles, administración, reportes y notificaciones en vivo por WebSocket.

**Stack:** Java 21 · Spring Boot 4.1 · PostgreSQL · Flyway · Spring Security (JWT) · JPA/Hibernate · JdbcTemplate (reportes) · STOMP/WebSocket · springdoc (Swagger UI).

## Puesta en marcha

1. Crear la base en PostgreSQL: `CREATE DATABASE restaurante;`
2. Editar [`.env`](./.env) (no se versiona; la plantilla es [`.env.example`](./.env.example)). **Cambie `DB_PASSWORD`** por la contraseña real de su usuario de PostgreSQL.
3. Arrancar:

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway crea el esquema al iniciar y, si no hay usuarios, se crea el administrador con `INITIAL_ADMIN_*`. Hibernate solo **valida** el esquema (`ddl-auto: validate`).

Documentación interactiva: <http://localhost:8080/swagger-ui.html> (en producción deshabilítela con `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false`).

### Variables del `.env`

| Variable | Uso |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a PostgreSQL |
| `JWT_SECRET` | Clave HMAC-SHA256 (mínimo 32 caracteres; la app no arranca con una más corta) |
| `JWT_EXPIRATION_MINUTES` | Vigencia del token |
| `INITIAL_ADMIN_NAME/EMAIL/PASSWORD` | Administrador inicial (solo si no existen usuarios) |
| `SERVER_PORT`, `CORS_ALLOWED_ORIGINS`, `APP_TIMEZONE` | Servidor, CORS/WebSocket y zona de los reportes |
| `UPLOADS_DIR` | Carpeta de las fotos de productos con `STORAGE_PROVIDER=local` (por defecto `./uploads`, no se versiona) |
| `STORAGE_PROVIDER` | Dónde se guardan las fotos: `local` (carpeta), `db` (tabla `imagenes` en PostgreSQL) o `r2` (Cloudflare R2, requiere `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`) |
| `SWAGGER_ENABLED` | `false` apaga la documentación interactiva (recomendado en producción) |
| `DB_POOL_SIZE` | Conexiones máximas a la base (5 en servidores pequeños) |
| `PORT` | Puerto que asignan plataformas como Render (tiene prioridad sobre `SERVER_PORT`) |

Una variable de entorno del sistema con el mismo nombre tiene prioridad sobre el `.env`, que es lo que debe usarse en producción.

## Probar la API con Postman

1. Arranque el backend (`.\mvnw.cmd spring-boot:run`).
2. En Postman: **Import** → [`postman/PoliRestaurante_API.postman_collection.json`](./postman/PoliRestaurante_API.postman_collection.json).
3. En la colección → pestaña **Variables**, ponga en `adminPassword` la contraseña con la que se creó el administrador (`INITIAL_ADMIN_PASSWORD` del `.env` la primera vez que arrancó la app).
4. Ejecute las carpetas en orden **01 → 06** (clic derecho en la colección → *Run collection*, o request por request). Los tokens y los ids (`pedidoId`, `productoId`, ...) se guardan solos como variables.

Cada request incluye una prueba que verifica el código HTTP esperado (incluidos los casos que deben fallar: 401, 403, 409, 422). Cada ejecución crea datos nuevos (usuarios, productos, mesas, pedidos), así que conviene correrla contra una base de desarrollo. El WebSocket no se puede probar desde esta colección; use una herramienta STOMP o el cliente del frontend.

## Pruebas automáticas

```powershell
.\mvnw.cmd test
```

Las pruebas de integración levantan la aplicación completa contra un **PostgreSQL real embebido** (no requiere Docker ni instalación): migraciones, seguridad por rol, flujo de pedidos, reglas de contraentrega, reportes y WebSocket.

## Roles y reglas

| Rol | Puede |
|---|---|
| `MESERO` | Ver menú y mesas libres; crear pedidos (salón, para llevar, domicilio); ver **sus** pedidos; editar productos/cantidades/notas **solo en PENDIENTE y si es el propietario**; entregar (LISTO → ENTREGADO) y cancelar (PENDIENTE/EN_PREPARACION) |
| `COCINA` | Ver la cola (PENDIENTE y EN_PREPARACION); PENDIENTE → EN_PREPARACION → LISTO |
| `ADMINISTRADOR` | Gestionar usuarios, menú, mesas y límite de contraentrega; consultar pedidos y reportes. **No cambia estados** |

Transiciones: `PENDIENTE → EN_PREPARACION → LISTO → ENTREGADO`, `PENDIENTE → CANCELADO`, `EN_PREPARACION → CANCELADO`. Cada cambio queda en `historial_estados` con fecha y usuario.

- Al crear un pedido de salón la mesa pasa a `OCUPADA`; al entregarlo o cancelarlo vuelve a `LIBRE`. Una mesa no puede tener dos pedidos activos (lock pesimista + índice único parcial).
- El total = Σ(cantidad × precio guardado en el detalle) + costo de envío. Cambiar el precio de un producto no altera pedidos existentes.
- No se puede eliminar el último producto de un pedido.
- Domicilio: exige dirección, teléfono y método de pago (`EFECTIVO_CONTRAENTREGA` o `PAGO_VIRTUAL`). Si el total supera el límite configurado (inicial **$80.000**) se rechaza la contraentrega. No se procesa ningún pago.
- Siempre debe existir al menos un administrador activo.

## Endpoints

Todos requieren `Authorization: Bearer <token>` salvo el login. Errores en formato RFC 7807 (`application/problem+json`): 400 validación, 401, 403, 404, 409 conflicto, 422 regla de negocio, 429 demasiados intentos.

| Área | Rutas | Rol |
|---|---|---|
| Autenticación | `POST /api/auth/login`, `GET /api/auth/me` | público / cualquiera |
| Menú | `GET /api/menu/categorias`, `GET /api/menu/productos?categoriaId=` | cualquiera |
| Portada pública | `GET /api/public/menu`, `GET /api/public/imagenes/{archivo}` | **público** (solo lectura) |
| Mesas | `GET /api/mesas/disponibles` | mesero |
| Pedidos | `POST /api/pedidos`, `GET /api/pedidos/{id}`, `GET /api/pedidos/{id}/historial`, `GET /api/pedidos/mios`, `GET /api/pedidos/cocina`, `PATCH /api/pedidos/{id}/estado` | según regla |
| Detalles | `POST /api/pedidos/{id}/detalles`, `PUT/DELETE /api/pedidos/{pedidoId}/detalles/{detalleId}` | mesero propietario |
| Admin menú | `GET/POST /api/admin/categorias`, `PUT/DELETE /api/admin/categorias/{id}`; igual para `/productos` y `/mesas` | admin |
| Fotos de productos | `PUT /api/admin/productos/{id}/imagen` (multipart, campo `archivo`), `DELETE /api/admin/productos/{id}/imagen` | admin |
| Admin usuarios | `POST /api/usuarios`, `GET /api/admin/usuarios`, `PUT /{id}`, `PATCH /{id}/estado`, `PATCH /{id}/contrasena` | admin |
| Admin pedidos | `GET /api/admin/pedidos` (estado, tipo, meseroId, mesaId, desde, hasta, page, size, sort), `GET /api/admin/pedidos/{id}` | admin |
| Configuración | `GET/PUT /api/admin/configuracion/domicilios` | admin |
| Reportes | `GET /api/admin/reportes/ventas`, `/productos-mas-vendidos`, `/cancelaciones`, `/tiempo-preparacion` (`desde`, `hasta` en `yyyy-MM-dd`; por defecto últimos 30 días, máx. 366) | admin |

Ejemplo de pedido a domicilio:

```json
{
  "tipo": "DOMICILIO",
  "nombreCliente": "Ana",
  "entrega": { "direccion": "Calle 1 # 2-3", "telefono": "3001234567", "costoEnvio": 5000, "metodoPago": "PAGO_VIRTUAL" },
  "items": [ { "productoId": 1, "cantidad": 2, "notas": "sin cebolla" } ]
}
```

## Notificaciones en vivo (WebSocket / STOMP)

Conexión: `ws://localhost:8080/ws`. En el frame `CONNECT` envíe la cabecera `Authorization: Bearer <token>`. El canal es solo de salida: los clientes no pueden enviar mensajes.

| Destino | Quién | Contenido |
|---|---|---|
| `/topic/cocina` | cocina, admin | pedido creado, editado o con cambio de estado |
| `/user/queue/pedidos` | cada mesero | eventos de **sus** pedidos (p. ej. cocina lo marcó LISTO) |
| `/topic/admin/pedidos` | admin | todos los eventos de pedidos |
| `/topic/mesas` | mesero, admin | mesas que se ocupan o se liberan |

Mensaje: `{"tipo":"PEDIDO_ESTADO_CAMBIADO","pedidoId":7,"estado":"LISTO","estadoAnterior":"EN_PREPARACION","tipoPedido":"SALON","meseroId":3,"mesaNumero":5,"fecha":"..."}`. El evento es liviano: consulte el detalle con `GET /api/pedidos/{id}`. Los mensajes se publican **después** de confirmar la transacción.

## Decisiones y límites conocidos

- **Fotos:** solo JPG, PNG y WebP de hasta 2 MB. El tipo se detecta por los bytes del archivo (no por su nombre), el nombre lo genera el servidor (UUID) y SVG no se acepta. Se guardan según `STORAGE_PROVIDER`: en disco (hay que respaldar la carpeta), en la base de datos (sirve en servidores de disco efímero; ocupa espacio de la base) o en R2 (sin servicios del servidor, requiere medio de pago en Cloudflare). Con varias instancias del backend no uses `local`.
- **Menú público:** `GET /api/public/menu` solo expone nombre, descripción, precio y foto de lo que está disponible. Sigue sin existir venta pública: crear pedidos exige rol de mesero.
- **Escalabilidad:** el broker WebSocket y el bloqueo de intentos de login viven en memoria; con varias instancias hay que usar un broker externo (RabbitMQ) y Redis respectivamente.
- Entregar y cancelar exigen ser el mesero propietario (igual que editar).
- Las ventas son los pedidos `ENTREGADO`, por fecha de entrega, en la zona `APP_TIMEZONE`.
- El token WebSocket se valida al conectar; una conexión abierta no se corta si el token vence después.
- Detrás de un proxy inverso (Render, Cloudflare…) el servidor ve la IP del proxy, no la del visitante. El bloqueo de login usa correo + IP, así que en la práctica funciona **por correo**. No active `server.forward-headers-strategy` salvo que el proxy sobrescriba esas cabeceras: si no, cualquiera podría falsificarlas y saltarse el bloqueo.
- Despliegue gratis para una demo: ver [DESPLIEGUE.md](../DESPLIEGUE.md).
- Fuera de alcance: frontend, pago virtual real, pedidos públicos de clientes y despliegue en contenedores.
