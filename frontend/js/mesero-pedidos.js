import { api } from "./api.js";
import { abrirDialogo, avisar, crearPaso, el, mensajeDeError, moneda, tiempoRelativo } from "./ui.js";
import { NOMBRE_ESTADO, NOMBRE_PAGO, destino, hora, insignia } from "./pedidos-comun.js";

const $ = (id) => document.getElementById(id);

const ACTIVOS = ["PENDIENTE", "EN_PREPARACION", "LISTO"];
const FILTROS = [
  { id: "activos", nombre: "Activos" },
  { id: "LISTO", nombre: "Listos" },
  { id: "ENTREGADO", nombre: "Entregados" },
  { id: "CANCELADO", nombre: "Cancelados" },
  { id: "todos", nombre: "Todos" },
];

export function iniciarPedidos({ productos, alActualizarConteo }) {
  let pedidos = [];
  let filtro = "activos";
  let abierto = null; // id del pedido que muestra el diálogo
  let datos = null; // { pedido, historial }
  let confirmandoCancelacion = false;
  let trabajando = false;
  let cerrarDialogo = () => {};
  const yaVistos = new Set();
  const porDestacar = new Set();

  const dialogo = $("dialogo-pedido");
  const contenido = $("dialogo-contenido");

  // ------------------------------------------------------------------ lista
  const coincide = (p) => (filtro === "todos" ? true : filtro === "activos" ? ACTIVOS.includes(p.estado) : p.estado === filtro);

  function pintarFiltros() {
    const cont = $("filtros-pedidos");
    cont.replaceChildren(...FILTROS.map((f) => {
      const cantidad = pedidos.filter((p) => (f.id === "todos" ? true : f.id === "activos" ? ACTIVOS.includes(p.estado) : p.estado === f.id)).length;
      const chip = el("button", "chip", `${f.nombre} · ${cantidad}`);
      chip.type = "button";
      chip.setAttribute("aria-pressed", String(f.id === filtro));
      chip.addEventListener("click", () => { filtro = f.id; yaVistos.clear(); pintarFiltros(); pintarLista(); });
      return chip;
    }));
  }

  function crearTarjeta(p, indice) {
    const tarjeta = el("article", `pedido ${p.estado}`);
    tarjeta.tabIndex = 0;
    tarjeta.setAttribute("role", "button");
    tarjeta.setAttribute("aria-label", `Ver pedido ${p.id}`);
    tarjeta.dataset.id = p.id;
    if (!yaVistos.has(p.id)) tarjeta.style.animationDelay = `${Math.min(indice, 8) * 50}ms`;
    else tarjeta.style.animation = "none";

    const cabecera = el("div", "pedido-cabecera");
    cabecera.append(el("strong", "numero", `#${p.id}`), insignia(p.estado));
    tarjeta.append(cabecera, el("div", "destino", destino(p)));

    const meta = el("div", "meta");
    meta.append(el("span", "", tiempoRelativo(p.createdAt)), el("span", "total-pedido", moneda.format(p.total)));
    tarjeta.append(meta);

    if (p.estado === "LISTO") {
      const acciones = el("div", "acciones");
      const boton = el("button", "btn btn-ok", "Marcar entregado");
      boton.type = "button";
      boton.addEventListener("click", (e) => { e.stopPropagation(); cambiarEstado(p.id, "ENTREGADO", null, boton); });
      acciones.append(boton);
      tarjeta.append(acciones);
    }

    const abrir = () => abrirDetalle(p.id);
    tarjeta.addEventListener("click", abrir);
    tarjeta.addEventListener("keydown", (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); abrir(); } });
    return tarjeta;
  }

  function pintarLista() {
    const lista = pedidos.filter(coincide);
    const cont = $("lista-pedidos");
    if (!lista.length) {
      cont.replaceChildren(el("p", "vacio", filtro === "activos" ? "No tienes pedidos activos. ¡Crea uno nuevo!" : "No hay pedidos en esta categoría."));
      return;
    }
    cont.replaceChildren(...lista.map(crearTarjeta));
    lista.forEach((p) => yaVistos.add(p.id));
    for (const id of porDestacar) {
      const nodo = cont.querySelector(`[data-id="${id}"]`);
      if (nodo) { nodo.style.animation = ""; nodo.classList.add("destacar"); }
    }
    porDestacar.clear();
  }

  async function cargar() {
    try {
      pedidos = (await api("GET", "/api/pedidos/mios?size=100")).content;
    } catch (e) {
      avisar(e.message, "error");
      return;
    }
    pintarFiltros();
    pintarLista();
    alActualizarConteo({
      activos: pedidos.filter((p) => ACTIVOS.includes(p.estado)).length,
      listos: pedidos.filter((p) => p.estado === "LISTO").length,
    });
  }

  setInterval(() => { if (!$("panel-mios").hidden) pintarLista(); }, 60000);

  // --------------------------------------------------------------- acciones
  async function cambiarEstado(id, estado, comentario, boton) {
    if (trabajando) return;
    trabajando = true;
    boton?.classList.add("cargando");
    try {
      await api("PATCH", `/api/pedidos/${id}/estado`, { estado, comentario });
      avisar(estado === "ENTREGADO" ? `Pedido #${id} entregado` : `Pedido #${id} cancelado`);
      confirmandoCancelacion = false;
      await cargar();
      if (abierto === id) await refrescarDetalle();
    } catch (e) {
      avisar(mensajeDeError(e), "error");
      await cargar();
      if (abierto === id) await refrescarDetalle();
    } finally {
      trabajando = false;
      boton?.classList.remove("cargando");
    }
  }

  /** Ejecuta una edición de productos y vuelve a pintar el detalle con la respuesta. */
  async function editar(peticion) {
    if (trabajando) return;
    trabajando = true;
    try {
      datos.pedido = await peticion();
      await cargar();
    } catch (e) {
      avisar(mensajeDeError(e), "error");
      await refrescarDetalle();
    } finally {
      trabajando = false;
      renderDetalle();
    }
  }

  const rutaDetalle = (d) => `/api/pedidos/${abierto}/detalles/${d.id}`;
  const guardarLinea = (d, cantidad, notas) => editar(() => api("PUT", rutaDetalle(d), { cantidad, notas: notas?.trim() || null }));
  const quitarLinea = (d) => editar(() => api("DELETE", rutaDetalle(d)));
  const agregarProducto = (productoId) => editar(() => api("POST", `/api/pedidos/${abierto}/detalles`, { productoId, cantidad: 1 }));

  // ------------------------------------------------------- diálogo de detalle
  async function refrescarDetalle() {
    try {
      const [pedido, historial] = await Promise.all([
        api("GET", `/api/pedidos/${abierto}`),
        api("GET", `/api/pedidos/${abierto}/historial`),
      ]);
      datos = { pedido, historial };
      renderDetalle();
    } catch (e) {
      avisar(e.message, "error");
    }
  }

  async function abrirDetalle(id) {
    abierto = id;
    confirmandoCancelacion = false;
    try {
      const [pedido, historial] = await Promise.all([api("GET", `/api/pedidos/${id}`), api("GET", `/api/pedidos/${id}/historial`)]);
      datos = { pedido, historial };
    } catch (e) {
      avisar(e.message, "error");
      return;
    }
    renderDetalle();
    cerrarDialogo = abrirDialogo(dialogo);
    dialogo.addEventListener("close", () => { abierto = null; datos = null; }, { once: true });
  }

  function bloqueProductos(p, editable) {
    const bloque = el("section", "bloque");
    bloque.append(el("h3", "", "Productos"));
    const unico = p.detalles.length === 1;

    for (const d of p.detalles) {
      const fila = el("div", "detalle-linea");
      const izquierda = el("div");
      izquierda.append(el("div", "nombre", d.productoNombre), el("div", "precio-linea", `${moneda.format(d.precioUnitario)} c/u · ${moneda.format(d.subtotal)}`));
      fila.append(izquierda);

      if (editable) {
        const herramientas = el("div", "herramientas");
        const paso = crearPaso(d.cantidad, {
          minimo: 1,
          alRestar: () => guardarLinea(d, d.cantidad - 1, d.notas),
          alSumar: () => guardarLinea(d, d.cantidad + 1, d.notas),
        });
        paso.fijar(d.cantidad);
        const quitar = el("button", "quitar", "✕");
        quitar.type = "button";
        quitar.disabled = unico;
        quitar.title = unico ? "Un pedido necesita al menos un producto" : "Quitar producto";
        quitar.setAttribute("aria-label", `Quitar ${d.productoNombre}`);
        quitar.addEventListener("click", () => quitarLinea(d));
        herramientas.append(paso.nodo, quitar);
        fila.append(herramientas);

        const nota = el("input", "nota");
        nota.type = "text";
        nota.maxLength = 255;
        nota.value = d.notas ?? "";
        nota.placeholder = "Nota (ej. sin cebolla)";
        nota.setAttribute("aria-label", `Nota para ${d.productoNombre}`);
        nota.addEventListener("change", () => { if ((d.notas ?? "") !== nota.value.trim()) guardarLinea(d, d.cantidad, nota.value); });
        fila.append(nota);
      } else {
        fila.append(el("span", "estado ENTREGADO", `× ${d.cantidad}`));
        if (d.notas) fila.append(el("p", "nota-texto", `Nota: ${d.notas}`));
      }
      bloque.append(fila);
    }

    if (editable) {
      const agregar = el("div", "agregar-producto");
      const select = el("select");
      select.setAttribute("aria-label", "Producto a agregar");
      select.append(new Option("Agregar otro producto…", ""));
      productos().forEach((prod) => select.append(new Option(`${prod.nombre} · ${moneda.format(prod.precio)}`, String(prod.id))));
      const boton = el("button", "btn btn-suave", "Agregar");
      boton.type = "button";
      boton.addEventListener("click", () => { if (select.value) agregarProducto(Number(select.value)); });
      agregar.append(select, boton);
      bloque.append(agregar);
    }
    return bloque;
  }

  function bloqueEntrega(p) {
    if (!p.entrega) return null;
    const bloque = el("section", "bloque");
    bloque.append(el("h3", "", "Entrega"));
    const caja = el("div", "datos-entrega");
    caja.append(
      el("span", "", `📍 ${p.entrega.direccion}`),
      el("span", "", `📞 ${p.entrega.telefono}`),
      el("span", "", `🛵 Envío: ${moneda.format(p.entrega.costoEnvio)}`),
      el("span", "", `💳 ${NOMBRE_PAGO[p.entrega.metodoPago] ?? p.entrega.metodoPago}`),
    );
    bloque.append(caja);
    return bloque;
  }

  function bloqueCronologia(historial) {
    const bloque = el("section", "bloque");
    bloque.append(el("h3", "", "Historial"));
    const lista = el("ol", "cronologia");
    historial.forEach((h, i) => {
      const item = el("li");
      item.style.animationDelay = `${i * 60}ms`;
      item.append(el("strong", "", NOMBRE_ESTADO[h.estadoNuevo] ?? h.estadoNuevo));
      item.append(el("small", "", `${hora(h.fecha)} · ${h.usuario.nombre}`));
      if (h.comentario) item.append(el("small", "comentario", `“${h.comentario}”`));
      lista.append(item);
    });
    bloque.append(lista);
    return bloque;
  }

  function renderDetalle() {
    if (!datos) return;
    const { pedido: p, historial } = datos;
    const editable = p.estado === "PENDIENTE";

    const cabecera = el("div", "dialogo-cabecera");
    const titulo = el("h2", "", `Pedido #${p.id}`);
    titulo.id = "dialogo-titulo";
    const cerrar = el("button", "cerrar-x", "✕");
    cerrar.type = "button";
    cerrar.setAttribute("aria-label", "Cerrar");
    cerrar.addEventListener("click", () => cerrarDialogo());
    cabecera.append(titulo, insignia(p.estado), el("span", "usuario", destino(p)), cerrar);

    const cuerpo = el("div", "dialogo-cuerpo");
    if (editable) cuerpo.append(el("p", "aviso info", "Mientras el pedido esté pendiente puedes cambiar productos, cantidades y notas."));
    cuerpo.append(bloqueProductos(p, editable));
    const total = el("div", "resumen-total");
    total.append(el("span", "", "Total"), el("strong", "", moneda.format(p.total)));
    cuerpo.append(total);
    const entrega = bloqueEntrega(p);
    if (entrega) cuerpo.append(entrega);
    cuerpo.append(bloqueCronologia(historial));

    if (confirmandoCancelacion) {
      const confirmar = el("div", "confirmar-cancelar");
      confirmar.append(el("strong", "", "¿Cancelar este pedido?"));
      const campo = el("label", "campo");
      campo.append(el("span", "", "Motivo (opcional)"));
      const motivo = el("input");
      motivo.type = "text";
      motivo.maxLength = 255;
      motivo.placeholder = "Ej. El cliente se fue";
      campo.append(motivo);
      const botones = el("div", "dialogo-pie");
      botones.style.cssText = "padding:0;border:0;background:none";
      const volver = el("button", "btn btn-suave", "Volver");
      volver.type = "button";
      volver.addEventListener("click", () => { confirmandoCancelacion = false; renderDetalle(); });
      const si = el("button", "btn btn-peligro", "Sí, cancelar pedido");
      si.type = "button";
      si.addEventListener("click", () => cambiarEstado(p.id, "CANCELADO", motivo.value.trim() || null, si));
      botones.append(volver, si);
      confirmar.append(campo, botones);
      cuerpo.append(confirmar);
    }

    const pie = el("div", "dialogo-pie");
    if (p.estado === "LISTO") {
      const entregar = el("button", "btn btn-ok", "Marcar entregado");
      entregar.type = "button";
      entregar.addEventListener("click", () => cambiarEstado(p.id, "ENTREGADO", null, entregar));
      pie.append(entregar);
    }
    if (ACTIVOS.includes(p.estado) && p.estado !== "LISTO" && !confirmandoCancelacion) {
      const cancelar = el("button", "btn btn-fantasma", "Cancelar pedido");
      cancelar.type = "button";
      cancelar.addEventListener("click", () => { confirmandoCancelacion = true; renderDetalle(); });
      pie.append(cancelar);
    }
    const cerrarPie = el("button", "btn btn-suave", "Cerrar");
    cerrarPie.type = "button";
    cerrarPie.addEventListener("click", () => cerrarDialogo());
    pie.append(cerrarPie);

    contenido.replaceChildren(cabecera, cuerpo, pie);
  }

  // ---------------------------------------------------------- tiempo real
  let temporizador;
  return {
    cargar,
    /** Tras crear un pedido se vuelve al filtro "Activos" para que se vea el pedido nuevo. */
    mostrarActivos() { filtro = "activos"; yaVistos.clear(); },
    destacar(id) { porDestacar.add(id); },
    /** Llega un evento de /user/queue/pedidos: se agrupa la recarga y se actualiza el detalle abierto. */
    alEvento(evento) {
      clearTimeout(temporizador);
      temporizador = setTimeout(async () => {
        await cargar();
        if (abierto === evento.pedidoId && !trabajando) await refrescarDetalle();
      }, 250);
    },
    abrir: abrirDetalle,
  };
}
