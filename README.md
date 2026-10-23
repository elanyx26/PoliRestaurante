# Sistema de Gestión de Restaurante

Aplicación en **Java** con base de datos **MySQL** para administrar un restaurante: usuarios por rol, mesas, menú (categorías y productos) y el ciclo completo de los pedidos (en mesa, para llevar y a domicilio), desde que el mesero los toma hasta que se cierran y se reportan.

## Características

- **Roles de usuario:** Administrador, Mesero y Personal de cocina, con inicio de sesión y activación/desactivación de cuentas.
- **Gestión del administrador:** crear, listar, actualizar y eliminar usuarios, mesas, categorías y productos; asignar roles.
- **Pedidos de tres tipos:**
  - En mesa (ocupa y libera la mesa automáticamente)
  - Para llevar (con nombre del cliente)
  - A domicilio (dirección, teléfono y costo de envío)
- **Ciclo de vida del pedido:** crear → enviar a cocina → en preparación → listo → entregado → cerrado (también se puede cancelar antes de entregar)
- **Notas por producto** (por ejemplo "sin cebolla") y resumen con total del pedido
- **Cola de cocina:** la cocina ve los pedidos pendientes y los datos que necesita.
- **Historial** de cambios de estado por pedido y de pedidos por mesa
- **Reportes (administrador):** ventas y tiempos de preparación

## Tecnologías

- Java
- MySQL
- JDBC (conector MySQL en la carpeta `lib/`)


## Estructura del proyecto

Backend/
├── api/          Recursos REST y servidor (ServidorRest, PedidoRecurso, MesaRecurso, ProductoRecurso, ...)
├── app/          Puntos de entrada: Main (demo) y MenuConsola (el menú interactivo)
├── entidades/    Clases del modelo (Administrador, Mesero, PersonalCocina, Mesa, │                 Categoria, Producto, Pedido, DetallePedido, Domicilio)
├── dao/          Acceso a datos: un DAO por tabla (MesaDAO, PedidoDAO, ProductoDAO, ...) con el SQL y el CRUD
├── servicios/    Lógica de negocio (AdministradorService, MeseroService, │                 PersonalCocinaService, MesaService, CategoriaService, ProductoService, │                 PedidoService, DetallePedidoService, DomicilioService, │                 AutenticacionService)
├── util/         Apoyo: Conexion y Bd (acceso a MySQL), Constantes, Validar, Seguridad,
│                 Fechas, CambioEstado, ReporteVentas
├── lib/          Librerías externas (conector MySQL) y LEEME.txt
├── sql/          Scripts de base de datos
├── postman/      Pruebas de la API REST con Postman
├── out/          Clases compiladas
├── db.properties Configuración de la conexión a MySQL
└── README.txt

## Capas

La aplicación sigue la misma idea del ejemplo de clase (modelo → DAO → cliente):

| Capa | Carpeta | Responsabilidad |
|------|---------|-----------------|
| Modelo | `entidades/` | Clases con los datos (equivalente a `models.py`) |
| DAO | `dao/` | Solo SQL y CRUD de cada tabla: `crear`, `obtenerPorId`, `obtenerTodos`, `actualizar`, `eliminar` y consultas propias (equivalente a `PersonaDAO.py`) |
| Servicio | `servicios/` | Reglas de negocio y validaciones; llaman a los DAO y no contienen SQL |
| Cliente | `app/` y `api/` | Menú por consola, demo y API REST; consumen los servicios (equivalente a `Cliente.py`) |

Los DAO usan `util/Bd` y `util/Conexion` para abrir cada conexión.

## Requisitos

- JDK 21
- MySQL Server
- Conector JDBC de MySQL 

## Configuración de la base de datos

1. Ejecuta en MySQL el script `sql/BaseDatosPoliRestaurante.sql` para crear la base de datos `restaurante` y sus tablas.
2. Configura la conexión en el archivo `db.properties`, ubicado en la raíz de `Backend/`


Ajusta `user`, `password` y si es necesario según tu instalación de MySQL.

 `sql/extension_modelo.sql` es **opcional** la aplicación aplica esas modificaciones automáticamente la primera vez que se conecta (`util/Conexion`). Solo ejecútalo manualmente una única vez y antes de abrir la aplicación, si prefieres hacerlo a mano.


Scripts disponibles en `sql/`:

`BaseDatosPoliRestaurante.sql`  Crea la base de datos y las tablas 
`extension_modelo.sql`  activación de usuarios, envío a cocina e historial de estados (la app lo aplica sola) 
`reiniciar_datos.sql`  Borra los datos de ejemplo insertados por `app.Main` 



## Compilación y ejecución

Desde la raíz de `Backend/`, compila el código en la carpeta `out/`:

```
mkdir out
javac -encoding UTF-8 -cp "lib/*" -d out entidades/*.java dao/*.java servicios/*.java util/*.java app/*.java api/*.java
```

### 1. Demostración del flujo completo (`app.Main`)

Ejecuta de punta a punta las historias de usuario HU1 a HU26 contra la base de datos. **Inserta datos de ejemplo**; cada ejecución usa nombres únicos, así que se puede repetir.


En Windows usa `;` en lugar de `:`:


Para borrar los datos de ejemplo, ejecuta `sql/reiniciar_datos.sql` en MySQL.

### 2. Menú por consola (`app.MenuConsola`)

Menú interactivo para administrar el sistema:

java -cp "out:lib/*" app.MenuConsola


Opciones del menú principal:

| Opción  |  Módulo |
|---------|-------- |
|    1    | Administradores (crear, listar, actualizar, eliminar) |
|    2    | Meseros |
|    3    | Personal de cocina |
|    4    | Mesas |
|    5    | Categorías |
|    6    | Productos |
|    7    | Pedidos (crear, listar, ver resumen) |
|    0    | Salir |

Al actualizar un registro, presiona **Enter** para conservar el valor actual.

### 3. API REST (`api.ServidorRest`)

Servidor REST del módulo de administración.

java -cp "out:lib/*" api.ServidorRest


Recursos disponibles:

|        Endpoint        |      Recurso       |
|------------------------|--------------------|
| `/api/categorias`      | Categorías         |
| `/api/productos`       | Productos          |
| `/api/mesas`           | Mesas              |
| `/api/administradores` | Administradores    |
| `/api/meseros`         | Meseros            |
| `/api/cocina`          | Personal de cocina |                                      
| `/api/roles`           | Roles              |                         
| `/api/pedidos`         | Pedidos            |
| `/api/detalles`        | Detalle de pedidos |



## Roles y permisos

|        Rol         |                         Puede hacer                          |
|--------------------|--------------------------------------------------------------|
| Administrador      | Gestionar usuarios, mesas y menú; asignar roles; consultar   | │                    | reportes                                                     |
| Mesero             | Crear pedidos, enviarlos a cocina, entregarlos, cancelarlos y| │                    | cerrarlos                                                    |
| Personal de cocina | Ver la cola, iniciar preparación y marcar pedidos como listos|


## Autores

*Daniela Florez y Camila Pacheco*


