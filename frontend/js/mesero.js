import { api, cerrarSesion, exigirSesion, urlWebSocket } from "./api.js";
import { avisar } from "./ui.js";
import { iniciarNuevoPedido } from "./mesero-nuevo.js";
import { iniciarPedidos } from "./mesero-pedidos.js";

const sesion = exigirSesion("MESERO");
if (!sesion) throw new Error("Sin sesión de mesero");

const $ = (id) => document.getElementById(id);
$("usuario").textContent = sesion.usuario.nombre;
$("btn-salir").addEventListener("click", () => cerrarSesion());

// ----------------------------------------------------------------- pestañas
const PESTANAS = {
  nuevo: { tab: $("tab-nuevo"), panel: $("panel-nuevo") },
  mios: { tab: $("tab-mios"), panel: $("panel-mios") },
};

function mostrarPestana(nombre) {
  for (const [clave, { tab, panel }] of Object.entries(PESTANAS)) {
    const activa = clave === nombre;
    tab.setAttribute("aria-selected", String(activa));
    panel.hidden = !activa;
  }
  nuevo.mostrarBotonFlotante(nombre === "nuevo");
  if (nombre === "mios") pedidos.cargar();
}
Object.entries(PESTANAS).forEach(([clave, { tab }]) => tab.addEventListener("click", () => mostrarPestana(clave)));

// ------------------------------------------------------------------- sonido
let sonidoActivo = false;
let audio;
$("btn-sonido").addEventListener("click", () => {
  sonidoActivo = !sonidoActivo;
  $("btn-sonido").textContent = sonidoActivo ? "🔔 Avisos con sonido" : "🔕 Avisos sin sonido";
  $("btn-sonido").setAttribute("aria-pressed", String(sonidoActivo));
  // El navegador solo permite audio tras un gesto del usuario: este clic lo habilita.
  if (sonidoActivo) {
    audio = audio || new AudioContext();
    campanilla();
  }
});

function campanilla() {
  if (!sonidoActivo || !audio) return;
  [660, 990].forEach((frecuencia, i) => {
    const inicio = audio.currentTime + i * 0.16;
    const oscilador = audio.createOscillator();
    const volumen = audio.createGain();
    oscilador.type = "sine";
    oscilador.frequency.value = frecuencia;
    volumen.gain.setValueAtTime(0.22, inicio);
    volumen.gain.exponentialRampToValueAtTime(0.001, inicio + 0.5);
    oscilador.connect(volumen).connect(audio.destination);
    oscilador.start(inicio);
    oscilador.stop(inicio + 0.5);
  });
}

// ---------------------------------------------------------------- módulos
const pedidos = iniciarPedidos({
  productos: () => nuevo.productos(),
  alActualizarConteo({ activos, listos }) {
    const insignia = $("insignia-mios");
    insignia.hidden = activos === 0;
    insignia.textContent = String(activos);
    insignia.classList.toggle("listos", listos > 0);
    insignia.title = listos > 0 ? `${listos} listo(s) para entregar` : `${activos} pedido(s) activo(s)`;
  },
});

const nuevo = iniciarNuevoPedido({
  alCrear(pedido) {
    avisar(`Pedido #${pedido.id} enviado a cocina`);
    pedidos.mostrarActivos();
    mostrarPestana("mios");
  },
});

// ---------------------------------------------------------------- WebSocket
const indicador = $("conexion");
function mostrarConexion(estado, texto) {
  indicador.className = `conexion ${estado}`;
  $("conexion-texto").textContent = texto;
}

function alEventoDePedido(evento) {
  pedidos.alEvento(evento);
  if (evento.tipo !== "PEDIDO_ESTADO_CAMBIADO") return;
  const lugar = evento.mesaNumero ? ` (mesa ${evento.mesaNumero})` : "";
  if (evento.estado === "LISTO") {
    pedidos.destacar(evento.pedidoId);
    avisar(`🍽️ El pedido #${evento.pedidoId}${lugar} está listo para entregar`);
    campanilla();
  } else if (evento.estado === "EN_PREPARACION") {
    avisar(`👨‍🍳 Cocina empezó el pedido #${evento.pedidoId}${lugar}`);
  }
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
  cliente.subscribe("/user/queue/pedidos", (mensaje) => alEventoDePedido(JSON.parse(mensaje.body)));
  cliente.subscribe("/topic/mesas", () => nuevo.recargarMesas());
  // Tras (re)conectar se actualiza todo: pudo haber cambios mientras no había conexión.
  pedidos.cargar();
  nuevo.recargarMesas();
};
cliente.onWebSocketClose = () => mostrarConexion("desconectado", "Reconectando…");
cliente.onStompError = () => {
  mostrarConexion("desconectado", "Sin conexión en vivo");
  // Si el token venció, esta llamada recibe 401 y la sesión se cierra sola.
  api("GET", "/api/auth/me").catch(() => {});
};

pedidos.cargar();
cliente.activate();
