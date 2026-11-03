import { api, cerrarSesion, exigirSesion, urlWebSocket } from "./api.js";
import { avisar, el } from "./ui.js";

const sesion = exigirSesion("COCINA");
if (!sesion) throw new Error("Sin sesión de cocina");

const MINUTOS_ATENCION = 10; // desde aquí la tarjeta se resalta como "retrasada"
const MINUTOS_URGENTE = 20;
const MS_SALIDA = 350; // debe coincidir con la transición .saliendo del CSS

const columnas = {
  PENDIENTE: {
    lista: document.getElementById("lista-pendientes"),
    cuenta: document.getElementById("cuenta-pendientes"),
    mensajeVacio: "Sin pedidos pendientes",
  },
  EN_PREPARACION: {
    lista: document.getElementById("lista-preparacion"),
    cuenta: document.getElementById("cuenta-preparacion"),
    mensajeVacio: "Nada en preparación",
  },
};
const indicador = document.getElementById("conexion");
const textoConexion = document.getElementById("conexion-texto");
const botonSonido = document.getElementById("btn-sonido");

document.getElementById("usuario").textContent = sesion.usuario.nombre;
document.getElementById("btn-salir").addEventListener("click", () => cerrarSesion());

// ------------------------------------------------------------------ utilidades

function minutosDesde(fechaIso) {
  return Math.max(0, Math.floor((Date.now() - new Date(fechaIso).getTime()) / 60000));
}

function textoTiempo(minutos) {
  if (minutos < 1) return "hace menos de 1 min";
  if (minutos < 60) return `hace ${minutos} min`;
  return `hace ${Math.floor(minutos / 60)} h ${minutos % 60} min`;
}

function descripcionDestino(pedido) {
  switch (pedido.tipo) {
    case "SALON":
      return `Mesa ${pedido.mesa ? pedido.mesa.numero : "?"}`;
    case "PARA_LLEVAR":
      return `Para llevar · ${pedido.nombreCliente}`;
    default:
      return `Domicilio · ${pedido.nombreCliente}`;
  }
}

// ------------------------------------------------------------------- sonido

let sonidoActivo = false;
let audio;

botonSonido.addEventListener("click", () => {
  sonidoActivo = !sonidoActivo;
  botonSonido.textContent = sonidoActivo ? "🔔 Sonido encendido" : "🔕 Sonido apagado";
  botonSonido.setAttribute("aria-pressed", String(sonidoActivo));
  // El navegador solo permite audio tras un gesto del usuario: este clic lo habilita.
  if (sonidoActivo) {
    audio = audio || new AudioContext();
    pitar();
  }
});

function pitar() {
  if (!sonidoActivo || !audio) return;
  const oscilador = audio.createOscillator();
  const volumen = audio.createGain();
  oscilador.type = "sine";
  oscilador.frequency.value = 880;
  volumen.gain.setValueAtTime(0.25, audio.currentTime);
  volumen.gain.exponentialRampToValueAtTime(0.001, audio.currentTime + 0.6);
  oscilador.connect(volumen).connect(audio.destination);
  oscilador.start();
  oscilador.stop(audio.currentTime + 0.6);
}

// ----------------------------------------------------------------- tarjetas

function crearTarjeta(pedido, esNueva) {
  const tarjeta = el("article", "tarjeta" + (esNueva ? " nueva" : ""));
  tarjeta.dataset.creado = pedido.createdAt;

  const cabecera = el("header", "tarjeta-cabecera");
  cabecera.append(el("strong", "numero", `#${pedido.id}`), el("span", "destino", descripcionDestino(pedido)));
  tarjeta.append(cabecera, el("span", "tiempo"));

  const items = el("ul", "items");
  for (const detalle of pedido.detalles) {
    const linea = el("li", "item");
    linea.append(el("span", "cantidad", `${detalle.cantidad} ×`), el("span", "producto", detalle.productoNombre));
    if (detalle.notas) linea.append(el("p", "nota", `Nota: ${detalle.notas}`));
    items.append(linea);
  }
  tarjeta.append(items);

  const enPreparacion = pedido.estado === "EN_PREPARACION";
  const boton = el("button", "btn btn-accion " + (enPreparacion ? "btn-ok" : "btn-primario"),
    enPreparacion ? "Marcar LISTO" : "Iniciar preparación");
  boton.type = "button";
  boton.addEventListener("click", () => avanzar(pedido, enPreparacion ? "LISTO" : "EN_PREPARACION", boton));
  tarjeta.append(boton);

  actualizarTiempo(tarjeta);
  return tarjeta;
}

function actualizarTiempo(tarjeta) {
  const minutos = minutosDesde(tarjeta.dataset.creado);
  tarjeta.querySelector(".tiempo").textContent = textoTiempo(minutos);
  tarjeta.classList.toggle("urgente", minutos >= MINUTOS_URGENTE);
  tarjeta.classList.toggle("atencion", minutos >= MINUTOS_ATENCION && minutos < MINUTOS_URGENTE);
}

setInterval(() => document.querySelectorAll(".tarjeta:not(.saliendo)").forEach(actualizarTiempo), 30000);

/** Resume lo que se ve en una tarjeta: si no cambia, el nodo del DOM se conserva (sin parpadeos). */
function firma(pedido) {
  const detalles = pedido.detalles.map((d) => `${d.id}:${d.cantidad}:${d.productoNombre}:${d.notas ?? ""}`);
  return `${pedido.estado}|${pedido.nombreCliente ?? ""}|${detalles.join(";")}`;
}

// ------------------------------------------------- reconciliación del tablero

const tarjetasPorId = new Map(); // id -> { nodo, firma, estado }
const conocidos = new Set();
let primeraCarga = true;

function retirar(nodo) {
  nodo.classList.add("saliendo");
  setTimeout(() => nodo.remove(), MS_SALIDA);
}

function actualizarCuenta(elemento, valor) {
  if (elemento.textContent === String(valor)) return;
  elemento.textContent = valor;
  elemento.classList.add("cambio");
  setTimeout(() => elemento.classList.remove("cambio"), 300);
}

function pintar(pedidos) {
  const idsActuales = new Set(pedidos.map((p) => p.id));

  for (const [id, registro] of tarjetasPorId) {
    if (!idsActuales.has(id)) {
      retirar(registro.nodo);
      tarjetasPorId.delete(id);
    }
  }

  let hayNuevos = false;
  for (const pedido of pedidos) {
    const actual = tarjetasPorId.get(pedido.id);
    const huella = firma(pedido);
    if (actual && actual.firma === huella) continue;

    if (actual) retirar(actual.nodo);
    const esNueva = !primeraCarga && !conocidos.has(pedido.id);
    if (esNueva) hayNuevos = true;
    tarjetasPorId.set(pedido.id, { nodo: crearTarjeta(pedido, esNueva), firma: huella, estado: pedido.estado });
    conocidos.add(pedido.id);
  }

  for (const [estado, columna] of Object.entries(columnas)) {
    const deseados = pedidos.filter((p) => p.estado === estado).map((p) => tarjetasPorId.get(p.id).nodo);
    const actuales = [...columna.lista.children].filter((n) => n.classList.contains("tarjeta") && !n.classList.contains("saliendo"));
    const mismoOrden = actuales.length === deseados.length && actuales.every((n, i) => n === deseados[i]);
    if (!mismoOrden) deseados.forEach((n) => columna.lista.append(n));

    columna.lista.querySelector(".vacio")?.remove();
    if (!deseados.length) columna.lista.prepend(el("p", "vacio", columna.mensajeVacio));
    actualizarCuenta(columna.cuenta, deseados.length);
  }

  primeraCarga = false;
  return hayNuevos;
}

// ------------------------------------------------------------------- datos

async function cargarCola() {
  try {
    pintar(await api("GET", "/api/pedidos/cocina"));
  } catch (e) {
    avisar(e.message, "error");
  }
}

let temporizadorRecarga;
/** Varios eventos seguidos (p. ej. creación + cambio de estado) se agrupan en una sola recarga. */
function programarRecarga() {
  clearTimeout(temporizadorRecarga);
  temporizadorRecarga = setTimeout(cargarCola, 250);
}

async function avanzar(pedido, nuevoEstado, boton) {
  boton.classList.add("cargando");
  try {
    await api("PATCH", `/api/pedidos/${pedido.id}/estado`, { estado: nuevoEstado });
  } catch (e) {
    // Típico: otra pantalla de cocina ya movió el pedido (409).
    avisar(`Pedido #${pedido.id}: ${e.message}`, "error");
  }
  await cargarCola();
}

// --------------------------------------------------------------- WebSocket

function mostrarConexion(estado, texto) {
  indicador.className = `conexion ${estado}`;
  textoConexion.textContent = texto;
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
  cliente.subscribe("/topic/cocina", (mensaje) => {
    const evento = JSON.parse(mensaje.body);
    if (evento.tipo === "PEDIDO_CREADO") pitar();
    programarRecarga();
  });
  // Tras (re)conectar se recarga la cola: pudo haber cambios mientras no había conexión.
  cargarCola();
};

cliente.onWebSocketClose = () => mostrarConexion("desconectado", "Reconectando…");

cliente.onStompError = () => {
  mostrarConexion("desconectado", "Sin conexión en vivo");
  // Si el token venció, esta llamada recibe 401 y la sesión se cierra sola.
  api("GET", "/api/auth/me").catch(() => {});
};

cargarCola();
cliente.activate();
