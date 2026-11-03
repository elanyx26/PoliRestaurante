import { api, cerrarSesion, exigirSesion, urlWebSocket } from "./api.js";
import { avisar, crearBrasas } from "./ui.js";
import { iniciarResumen } from "./admin-resumen.js";
import { iniciarPedidos } from "./admin-pedidos.js";
import { iniciarMenu } from "./admin-menu.js";
import { iniciarMesas } from "./admin-mesas.js";
import { iniciarUsuarios } from "./admin-usuarios.js";
import { iniciarConfiguracion } from "./admin-config.js";

const sesion = exigirSesion("ADMINISTRADOR");
if (!sesion) throw new Error("Sin sesión de administrador");

const $ = (id) => document.getElementById(id);
$("usuario").textContent = sesion.usuario.nombre;
$("btn-salir").addEventListener("click", () => cerrarSesion());
crearBrasas(10);

// Cada sección se construye la primera vez que se visita.
const FABRICAS = {
  resumen: (raiz) => iniciarResumen(raiz),
  pedidos: (raiz) => iniciarPedidos(raiz),
  menu: (raiz) => iniciarMenu(raiz),
  mesas: (raiz) => iniciarMesas(raiz),
  usuarios: (raiz) => iniciarUsuarios(raiz, { usuarioActualId: sesion.usuario.id }),
  configuracion: (raiz) => iniciarConfiguracion(raiz),
};
const vistas = {};
let actual = null;

function mostrar(nombre) {
  if (!FABRICAS[nombre]) nombre = "resumen";
  if (nombre === actual) return;
  actual = nombre;

  document.querySelectorAll(".admin-item").forEach((a) => {
    const activo = a.dataset.vista === nombre;
    a.classList.toggle("activo", activo);
    if (activo) { a.setAttribute("aria-current", "page"); a.scrollIntoView({ inline: "center", block: "nearest" }); } else a.removeAttribute("aria-current");
  });
  document.querySelectorAll(".vista").forEach((v) => {
    const es = v.id === `vista-${nombre}`;
    v.hidden = !es;
    v.classList.toggle("entrando", es);
  });

  const raiz = $(`vista-${nombre}`);
  if (!vistas[nombre]) vistas[nombre] = FABRICAS[nombre](raiz);
  vistas[nombre].mostrar();
  if (nombre === "pedidos") $("punto-pedidos").hidden = true;
  document.title = `Poli restaurante · ${nombre.charAt(0).toUpperCase()}${nombre.slice(1)}`;
}

function desdeHash() {
  mostrar(location.hash.replace("#", "") || "resumen");
}
window.addEventListener("hashchange", desdeHash);
desdeHash();

// ---------------------------------------------------------------- WebSocket
const indicador = $("conexion");
function mostrarConexion(estado, texto) {
  indicador.className = `conexion ${estado}`;
  $("conexion-texto").textContent = texto;
}

const cliente = new StompJs.Client({
  brokerURL: urlWebSocket(),
  connectHeaders: { Authorization: `Bearer ${sesion.token}` },
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
});

cliente.onConnect = () => {
  mostrarConexion("conectado", "En vivo");
  cliente.subscribe("/topic/admin/pedidos", (mensaje) => {
    const evento = JSON.parse(mensaje.body);
    vistas.pedidos?.actualizar();
    if (actual === "resumen") vistas.resumen?.actualizar();
    if (evento.tipo === "PEDIDO_CREADO") {
      if (actual !== "pedidos") $("punto-pedidos").hidden = false;
      avisar(`🧾 Nuevo pedido #${evento.pedidoId}`);
    }
  });
  cliente.subscribe("/topic/mesas", () => vistas.mesas?.actualizar());
};
cliente.onWebSocketClose = () => mostrarConexion("desconectado", "Reconectando…");
cliente.onStompError = () => {
  mostrarConexion("desconectado", "Sin conexión en vivo");
  // Si el token venció, esta llamada recibe 401 y la sesión se cierra sola.
  api("GET", "/api/auth/me").catch(() => {});
};
cliente.activate();
