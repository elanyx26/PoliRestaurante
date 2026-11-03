# Despliegue gratis en internet (sin dominio propio)

Pensado para **mostrar el proyecto** (una demo), no para atender clientes reales todos los días.

| Pieza | Servicio | Dirección que recibes | Costo | Tarjeta |
|---|---|---|---|---|
| Base de datos | **Neon** (PostgreSQL) | — | Gratis | No |
| Backend | **Render** (Web Service con Docker) | `https://poli-restaurante-api.onrender.com` | Gratis | No |
| Frontend | **Cloudflare Pages** | `https://poli-restaurante.pages.dev` | Gratis | No |
| Fotos | En la **base de datos** (`STORAGE_PROVIDER=db`) | — | Gratis | No |

> **Fotos en Cloudflare R2:** también está soportado (`STORAGE_PROVIDER=r2`), pero Cloudflare exige registrar un medio de pago (tarjeta, PayPal o billetera) para activarlo, aunque el uso gratuito (10 GB) alcance. El soporte R2 está probado con un servidor S3 simulado, **no contra Cloudflare real**. Para una demo sin tarjeta usa `db`.

Los precios y límites gratuitos cambian; confírmalos en cada sitio. Los nombres `poli-restaurante-api` y `poli-restaurante` son sugerencias: si ya están tomados, el servicio te dará otra dirección y tendrás que usarla en los pasos 3 y 4.

## Lo que debes saber antes de empezar

- **Render gratis duerme tras 15 minutos sin visitas.** Despertarlo tarda **entre 1 y 3 minutos o más** (Spring arranca con muy poca CPU). Para una demo: abre la dirección del backend 15 a 20 minutos antes y espera a que responda.
- Mientras el backend duerme se cortan las notificaciones en vivo; al despertar se reconectan solas.
- La base de Neon también se duerme a los 5 minutos sin uso, pero despierta en milisegundos y no requiere que hagas nada.
- Las fotos en la base ocupan espacio de tu 1 GB gratuito. Súbelas comprimidas (menos de 300 KB cada una).

## Paso 0: subir el código a GitHub

Render lee el código desde GitHub. Desde `C:\Daniela\PoliRestaurante`:

```powershell
git status                      # backend-spring/.env NO debe aparecer en la lista
git add backend-spring frontend DESPLIEGUE.md README.md
git commit -m "Backend Spring Boot, frontend y guía de despliegue"
git push
```

Comprueba antes de subir que `backend-spring/.env` no aparezca en `git status`: está en `.gitignore`, pero revísalo igual. Si el repositorio es **público**, cualquiera verá el código; la carpeta antigua `Backend/` incluye un `db.properties` con una contraseña, cámbiala o haz el repositorio privado.

## Paso 1: base de datos en Neon

1. Crea una cuenta en <https://neon.com> y un proyecto (región cercana, por ejemplo `AWS US East`).
2. En el panel del proyecto abre **Connect** y **desactiva "Pooled connection"** (usa la conexión directa).
3. Anota estos datos, que Neon muestra en la cadena de conexión `postgresql://USUARIO:CLAVE@HOST/BASE?sslmode=require`:
   - `DB_URL` → `jdbc:postgresql://HOST/BASE?sslmode=require` (empieza por `jdbc:postgresql://`, sin usuario ni clave)
   - `DB_USERNAME` → USUARIO
   - `DB_PASSWORD` → CLAVE

Las tablas las crea el backend solo al arrancar (Flyway).

## Paso 2: claves nuevas

Genera un `JWT_SECRET` nuevo (no reutilices el de tu `.env` local):

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Elige también una contraseña fuerte para el administrador inicial (`INITIAL_ADMIN_PASSWORD`).

## Paso 3: backend en Render

1. Crea una cuenta en <https://render.com> y conecta tu cuenta de GitHub.
2. **New → Web Service** → elige tu repositorio.
3. Configura:
   - **Name:** `poli-restaurante-api`
   - **Root Directory:** `backend-spring`
   - **Language / Runtime:** Docker (Render detecta el `Dockerfile`)
   - **Instance Type:** Free
   - **Health Check Path:** `/actuator/health`
4. En **Environment Variables** agrega:

| Variable | Valor |
|---|---|
| `DB_URL` | el del paso 1 |
| `DB_USERNAME` | el del paso 1 |
| `DB_PASSWORD` | el del paso 1 |
| `JWT_SECRET` | el generado en el paso 2 |
| `INITIAL_ADMIN_NAME` | `Administrador` |
| `INITIAL_ADMIN_EMAIL` | tu correo de administrador |
| `INITIAL_ADMIN_PASSWORD` | la contraseña del paso 2 |
| `STORAGE_PROVIDER` | `db` |
| `CORS_ALLOWED_ORIGINS` | `https://poli-restaurante.pages.dev` (la dirección del frontend, paso 4) |
| `APP_TIMEZONE` | `America/Bogota` |

   `SWAGGER_ENABLED=false` y `DB_POOL_SIZE=5` ya vienen en el `Dockerfile`.
5. **Create Web Service.** La primera compilación tarda varios minutos. Cuando termine, abre `https://poli-restaurante-api.onrender.com/actuator/health`: debe responder `{"groups":["liveness","readiness"],"status":"UP"}`.

## Paso 4: frontend en Cloudflare Pages

1. Abre `frontend/js/config.js` y pon en `BACKEND_PUBLICO` la dirección real del backend (sin barra al final). Guarda, haz commit y `git push`.
2. Crea una cuenta en <https://dash.cloudflare.com> → **Workers & Pages** → **Create** → **Pages** → **Upload assets** (subida directa).
3. Nombre del proyecto: `poli-restaurante`. Arrastra la carpeta **`frontend`** completa (puedes quitar `servir.py` y `README.md`, no hacen falta) y pulsa **Deploy**.
4. Abre la dirección que te da (`https://poli-restaurante.pages.dev`). Si es distinta de la que pusiste en `CORS_ALLOWED_ORIGINS`, corrígela en Render (se redespliega solo).

Cada vez que cambies el frontend, vuelve a subir la carpeta.

## Paso 5: datos de ejemplo (opcional)

Con el backend ya arrancado una vez, en Neon abre **SQL Editor**, pega el contenido de `backend-spring/deploy/datos-demo.sql` y ejecútalo: crea categorías, 10 platos y 6 mesas. Las fotos se suben después desde el panel del administrador.

## Paso 6: probar

1. Abre la dirección de Pages → **Ingresar** → entra con tu correo y contraseña de administrador.
2. **Usuarios**: crea un mesero y un cocinero con contraseñas propias.
3. **Menú**: sube fotos a los platos.
4. Abre el mesero y la cocina en otros navegadores (o en ventanas de incógnito) y haz un pedido de punta a punta.

## El día de la demo

- 20 minutos antes: abre `https://TU-BACKEND.onrender.com/actuator/health` hasta que responda `UP`, y luego carga la portada y haz login una vez en cada rol.
- Durante la demo la actividad lo mantiene despierto.
- Opcional: un servicio de "ping" gratuito (por ejemplo cron-job.org o UptimeRobot) que visite `/actuator/health` cada 10 minutos evita que duerma. No verifiqué que Render lo permita en sus términos, revísalo antes.

## Si algo falla

| Síntoma | Causa probable |
|---|---|
| La página abre pero el menú no carga o dice "No se pudo conectar" | El backend está dormido o la dirección de `BACKEND_PUBLICO` está mal (debe empezar por `https://`) |
| Error de CORS en la consola del navegador, o "Reconectando…" permanente | `CORS_ALLOWED_ORIGINS` no coincide exactamente con la dirección de Pages (con `https://` y sin `/` al final) |
| El backend no arranca (ver Logs en Render) | `DB_URL` mal escrito (debe empezar por `jdbc:postgresql://`), o usaste la conexión "pooled" de Neon |
| Render dice "Out of memory" | Reinicia el servicio; si se repite, baja `DB_POOL_SIZE` a 3 |
| Entras pero no puedes iniciar sesión con el administrador | El administrador solo se crea con la **primera** ejecución sobre una base vacía; la contraseña quedó la de ese momento |
| Las fotos no se ven | Revisa que `STORAGE_PROVIDER` sea `db` en Render |

## Al terminar

Para no dejar nada abierto, borra el servicio en Render, el proyecto en Neon y el proyecto en Cloudflare Pages.
