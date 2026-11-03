# PoliRestaurante · Frontend

HTML + CSS + JavaScript puros (módulos ES), sin paso de compilación. Consume la API de [`../backend-spring`](../backend-spring/README.md).

## Pantallas

| Archivo | Acceso | Descripción |
|---|---|---|
| `index.html` | público | Portada: carrusel de platos destacados, menú con categorías y búsqueda, y tipos de servicio. Lee `GET /api/public/menu`, sin iniciar sesión |
| `login.html` | personal | Inicio de sesión; redirige según el rol |
| `mesero.html` | `MESERO` | Crear pedidos (salón, para llevar, domicilio), ver y editar los propios, entregar y cancelar, con avisos en vivo |
| `cocina.html` | `COCINA` | Tablero de pedidos **Pendientes** y **En preparación**, en vivo por WebSocket |
| `admin.html` | `ADMINISTRADOR` | Panel de administración: resumen con reportes, pedidos, menú con fotos, mesas, usuarios y configuración |

Las tres pantallas del personal están completas.

## Panel del administrador
Navegación lateral (en celular, pestañas horizontales) con rutas por `#hash`: `#resumen`, `#pedidos`, `#menu`, `#mesas`, `#usuarios`, `#configuracion`. Cada sección se carga la primera vez que se visita.

- **Resumen:** ventas, ticket promedio, envíos y tiempo de preparación con números animados; gráfico de ventas por día, ventas por tipo, productos más vendidos y cancelaciones (por estado y por mesero). Rango rápido (hoy, 7 días, 30 días, este mes) o fechas a elección; por defecto, los últimos 30 días.
- **Pedidos:** lista paginada con filtros (estado, tipo, mesero, fechas) y detalle con productos, entrega e historial. Es solo de consulta: el administrador no cambia estados. Se actualiza sola y marca con un punto verde cuando llega un pedido nuevo.
- **Menú:** productos en tarjetas con foto, filtros, interruptor de disponibilidad y alta/edición/borrado. **La foto se sube desde el formulario** (arrastrando o eligiendo un archivo JPG, PNG o WebP de hasta 2 MB, con vista previa y opción de quitarla). Las categorías se administran en la otra pestaña.
- **Mesas:** alta, edición y borrado; el estado (libre/ocupada) lo controlan los pedidos y se actualiza en vivo.
- **Usuarios:** lista con búsqueda y filtros, alta, edición, activar/desactivar (corta el acceso al instante), cambio de contraseña y protección para no dejar el sistema sin administrador activo.
- **Configuración:** límite de pago contraentrega para domicilios.

Las tablas se convierten en tarjetas en pantallas angostas. Los errores del servidor (nombre duplicado, correo repetido, producto ya usado en pedidos…) se muestran dentro del mismo formulario o como aviso.

## Portada: transiciones y animaciones
- **Scroll:** barra de progreso de lectura; el hero se desvanece con *parallax* mientras el menú sube como una "hoja" con esquinas redondeadas; una franja diagonal se desliza con el scroll entre el menú y los servicios; los títulos aparecen palabra por palabra; la navegación marca la sección activa.
- **Tarjetas:** giro 3D escalonado al aparecer, inclinación con brillo que sigue al mouse, destello diagonal sobre la foto, precio que "salta" y salida animada al filtrar o buscar. Al abrir una tarjeta se muestra el detalle del plato en una ventana animada (Esc o clic fuera para cerrar).
- La inclinación solo se activa con mouse; todo respeta *reducir movimiento*.

## Pantalla del mesero
- **Nuevo pedido:** menú con categorías y buscador; "Agregar" lanza una miniatura volando hasta el carrito. El pedido actual se muestra a la derecha en escritorio y como hoja inferior (con botón flotante) en celular.
- **Tipos:** salón (elige una mesa libre), para llevar (nombre) y domicilio (dirección, teléfono, envío y método de pago). Si la contraentrega supera el límite, se muestra el mensaje del backend.
- **Mis pedidos:** filtros (activos, listos, entregados, cancelados, todos), detalle con historial (fecha y usuario) y, mientras el pedido esté **PENDIENTE**, edición de cantidades, notas y productos (no se puede quitar el último). Se puede cancelar (con motivo) en PENDIENTE o EN_PREPARACION y marcar entregado cuando está LISTO.
- **En vivo:** avisos cuando cocina empieza o termina un pedido (con sonido opcional), las mesas libres se actualizan solas y el detalle abierto se refresca.
- Se muestran los 100 pedidos más recientes del mesero.

## Cómo ejecutarlo

1. Arranque el backend (`backend-spring`: `.\mvnw.cmd spring-boot:run`).
2. Sirva esta carpeta en el puerto **5173**, que ya está permitido en `CORS_ALLOWED_ORIGINS` del backend:

```powershell
cd frontend
python servir.py
```

[`servir.py`](./servir.py) es un servidor estático que **desactiva la caché**, de modo que cada cambio en HTML, CSS o JS se ve al recargar (con `python -m http.server` el navegador puede mostrar archivos viejos).

3. Abra <http://localhost:5173>. Para el personal: botón **Ingresar** → usuario de rol `COCINA`.

Si el backend corre en otra dirección, cambie `API_URL` en [`js/config.js`](./js/config.js) y agregue el origen del frontend a `CORS_ALLOWED_ORIGINS`.

## Sistema de diseño (compartido por todas las pantallas)

Todo se define en [`css/base.css`](./css/base.css); [`css/panel.css`](./css/panel.css) agrega los componentes de las pantallas del personal (barra, estados de pedido, selector de cantidad, diálogos) y cada pantalla suma su propio archivo (`landing.css`, `login.css`, `mesero.css`, `cocina.css`, `admin.css`).

- **Colores:** fondo casi negro (`--bg`), **rosa/coral** como acento (`--acento`) y **morado** en detalles (`--morado`). Cámbielos en `:root` y toda la app se actualiza.
- **Tipografías:** Chelsea Market (títulos) y Comfortaa (texto), incluidas en [`fonts/`](./fonts) con su licencia OFL; no se descargan de internet.
- **Componentes reutilizables:** botones (`.btn-primario`, `.btn-fantasma`, `.btn-suave`, `.btn-ok`), campos de formulario (`.campo`), avisos, toast, esqueletos de carga y brasas animadas de fondo.
- **Movimiento:** aparición escalonada al hacer scroll (`.revelar`), carrusel con transiciones, hover con elevación, tarjetas de cocina que entran y salen con animación, y botón con indicador de carga. Todo se desactiva automáticamente si el sistema tiene activado *reducir movimiento*.
- **Responsive:** diseño fluido de móvil a escritorio (menú hamburguesa, rejillas que se adaptan, controles táctiles grandes).
- **Marca:** logo en [`img/llama.svg`](./img/llama.svg) (también favicon y emblema para platos sin foto).

## Pantalla de cocina
- La cola se actualiza sola cuando un mesero crea o edita un pedido, o cuando otra pantalla cambia un estado. El indicador **En vivo** muestra el estado del WebSocket y se reconecta solo.
- Botones **Iniciar preparación** y **Marcar LISTO**. Si otra pantalla ya movió el pedido se muestra el aviso y se recarga la cola.
- Las tarjetas se resaltan en ámbar a los 10 min y en rojo a los 20 min de espera; los pedidos nuevos entran con un destello.
- **Sonido** opcional al llegar un pedido nuevo (el navegador exige activarlo con un clic).

## Fotos de los platos
Se suben desde el panel del administrador (**Menú → editar producto**) o por la API (`PUT /api/admin/productos/{id}/imagen`, JPG/PNG/WebP de hasta 2 MB). Los productos sin foto muestran el emblema de la marca.

## Notas técnicas
- El token JWT se guarda en `sessionStorage` (se borra al cerrar la pestaña). Si vence, la app vuelve al login.
- Todo el contenido que viene de la API se pinta con `textContent`, nunca con `innerHTML`, para que notas o nombres no puedan inyectar HTML.
- La librería STOMP ([@stomp/stompjs](https://github.com/stomp-js/stompjs) 7.5.0, Apache-2.0) está incluida en `js/vendor/`; no se necesita internet.
