import { api } from "./api.js";
import { abrirDialogo, avisar, el, mensajeDeError, moneda } from "./ui.js";
import { NOMBRE_ESTADO, NOMBRE_PAGO, NOMBRE_TIPO, destino, hora, insignia } from "./pedidos-comun.js";
import { crearPaginacion, crearTabla, esqueletos, estadoVacio } from "./admin-ui.js";

const TAMANO = 12;

function crearSelect(opciones, etiqueta) {
  const s = el("select", "control");
  s.setAttribute("aria-label", etiqueta);
  opciones.forEach((o) => s.append(new Option(o.texto, o.valor)));
  return s;
}

export function iniciarPedidos(raiz) {
  let pagina = 0;
  let version = 0;
  let cargadoUnaVez = false;
  let temporizador;

  const cabecera = el("div", "vista-cabecera");
  const titulos = el("div");
  titulos.append(el("h1", "", "Pedidos"), el("p", "suave", "Consulta de todos los pedidos. El administrador no cambia estados."));
  cabecera.append(titulos);

  // -------------------------------------------------------------- filtros
  const filtros = el("div", "filtros-barra");
  const estado = crearSelect([{ valor: "", texto: "Todos los estados" }, ...Object.entries(NOMBRE_ESTADO).map(([v, t]) => ({ valor: v, texto: t }))], "Estado");
  const tipo = crearSelect([{ valor: "", texto: "Todos los tipos" }, ...Object.entries(NOMBRE_TIPO).map(([v, t]) => ({ valor: v, texto: t }))], "Tipo de pedido");
  const mesero = crearSelect([{ valor: "", texto: "Todos los meseros" }], "Mesero");
  const desde = el("input", "control");
  const hasta = el("input", "control");
  desde.type = hasta.type = "date";
  desde.setAttribute("aria-label", "Desde");
  hasta.setAttribute("aria-label", "Hasta");
  const limpiar = el("button", "btn btn-suave", "Limpiar");
  limpiar.type = "button";

  const grupoFechas = el("div", "rango-fechas");
  grupoFechas.append(desde, el("span", "", "→"), hasta);
  filtros.append(estado, tipo, mesero, grupoFechas, limpiar);

  const lista = el("div", "vista-lista");
  raiz.append(cabecera, filtros, lista);

  [estado, tipo, mesero, desde, hasta].forEach((c) => c.addEventListener("change", () => { pagina = 0; cargar(); }));
  limpiar.addEventListener("click", () => {
    [estado, tipo, mesero, desde, hasta].forEach((c) => { c.value = ""; });
    pagina = 0;
    cargar();
  });

  async function cargarMeseros() {
    try {
      const r = await api("GET", "/api/admin/usuarios?rol=MESERO&size=100&sort=nombre,asc");
      r.content.forEach((u) => mesero.append(new Option(u.nombre, String(u.id))));
    } catch {
      // El filtro por mesero es opcional: si falla, la lista sigue funcionando.
    }
  }

  // ---------------------------------------------------------------- lista
  function consulta() {
    const p = new URLSearchParams({ page: String(pagina), size: String(TAMANO) });
    if (estado.value) p.set("estado", estado.value);
    if (tipo.value) p.set("tipo", tipo.value);
    if (mesero.value) p.set("meseroId", mesero.value);
    if (desde.value) p.set("desde", desde.value);
    if (hasta.value) p.set("hasta", hasta.value);
    return p.toString();
  }

  async function cargar() {
    const mi = ++version;
    if (!cargadoUnaVez) lista.replaceChildren(esqueletos(6));
    try {
      const datos = await api("GET", `/api/admin/pedidos?${consulta()}`);
      if (mi !== version) return;
      cargadoUnaVez = true;
      if (!datos.content.length) {
        lista.replaceChildren(estadoVacio("No hay pedidos con estos filtros."));
        return;
      }
      const tabla = crearTabla([
        { titulo: "Pedido", render: (p) => el("strong", "numero-pedido", `#${p.id}`) },
        { titulo: "Fecha", render: (p) => hora(p.createdAt) },
        { titulo: "Destino", render: (p) => destino(p) },
        { titulo: "Mesero", render: (p) => p.mesero.nombre },
        { titulo: "Estado", render: (p) => insignia(p.estado) },
        { titulo: "Total", clase: "derecha", render: (p) => el("strong", "", moneda.format(p.total)) },
      ], datos.content, { alClicFila: (p) => abrirDetalle(p.id) });
      lista.replaceChildren(tabla, crearPaginacion({
        pagina: datos.page, totalPaginas: datos.totalPages, total: datos.totalElements,
        alCambiar: (n) => { pagina = n; cargar(); },
      }));
    } catch (e) {
      if (mi !== version) return;
      const reintentar = el("button", "btn btn-fantasma", "Reintentar");
      reintentar.type = "button";
      reintentar.addEventListener("click", cargar);
      lista.replaceChildren(estadoVacio(mensajeDeError(e), reintentar));
    }
  }

  // -------------------------------------------------------------- detalle
  async function abrirDetalle(id) {
    let pedido;
    let historial;
    try {
      [pedido, historial] = await Promise.all([api("GET", `/api/admin/pedidos/${id}`), api("GET", `/api/pedidos/${id}/historial`)]);
    } catch (e) {
      avisar(mensajeDeError(e), "error");
      return;
    }
    const caja = document.getElementById("dialogo-admin-contenido");
    let cerrar = () => {};

    const cab = el("div", "dialogo-cabecera");
    const titulo = el("h2", "", `Pedido #${pedido.id}`);
    titulo.id = "dialogo-admin-titulo";
    const x = el("button", "cerrar-x", "✕");
    x.type = "button";
    x.setAttribute("aria-label", "Cerrar");
    x.addEventListener("click", () => cerrar());
    cab.append(titulo, insignia(pedido.estado), el("span", "usuario", destino(pedido)), x);

    const cuerpo = el("div", "dialogo-cuerpo");
    const info = el("div", "datos-entrega");
    info.append(
      el("span", "", `👤 Mesero: ${pedido.mesero.nombre}`),
      el("span", "", `🕒 Creado: ${hora(pedido.createdAt)}`),
    );
    if (pedido.entregadoEn) info.append(el("span", "", `✅ Entregado: ${hora(pedido.entregadoEn)}`));
    if (pedido.entrega) {
      info.append(
        el("span", "", `📍 ${pedido.entrega.direccion}`),
        el("span", "", `📞 ${pedido.entrega.telefono}`),
        el("span", "", `🛵 Envío: ${moneda.format(pedido.entrega.costoEnvio)}`),
        el("span", "", `💳 ${NOMBRE_PAGO[pedido.entrega.metodoPago] ?? pedido.entrega.metodoPago}`),
      );
    }
    cuerpo.append(info);

    const productos = el("section", "bloque");
    productos.append(el("h3", "", "Productos"));
    pedido.detalles.forEach((d) => {
      const fila = el("div", "detalle-linea");
      const izq = el("div");
      izq.append(el("div", "nombre", d.productoNombre), el("div", "precio-linea", `${moneda.format(d.precioUnitario)} c/u · ${moneda.format(d.subtotal)}`));
      fila.append(izq, el("span", "estado ENTREGADO", `× ${d.cantidad}`));
      if (d.notas) fila.append(el("p", "nota-texto", `Nota: ${d.notas}`));
      productos.append(fila);
    });
    cuerpo.append(productos);

    const total = el("div", "resumen-total");
    total.append(el("span", "", "Total"), el("strong", "", moneda.format(pedido.total)));
    cuerpo.append(total);

    const hist = el("section", "bloque");
    hist.append(el("h3", "", "Historial"));
    const ol = el("ol", "cronologia");
    historial.forEach((h, i) => {
      const li = el("li");
      li.style.animationDelay = `${i * 60}ms`;
      li.append(el("strong", "", NOMBRE_ESTADO[h.estadoNuevo] ?? h.estadoNuevo), el("small", "", `${hora(h.fecha)} · ${h.usuario.nombre}`));
      if (h.comentario) li.append(el("small", "comentario", `“${h.comentario}”`));
      ol.append(li);
    });
    hist.append(ol);
    cuerpo.append(hist);

    const pie = el("div", "dialogo-pie");
    const cerrarBtn = el("button", "btn btn-suave", "Cerrar");
    cerrarBtn.type = "button";
    cerrarBtn.addEventListener("click", () => cerrar());
    pie.append(cerrarBtn);

    caja.replaceChildren(cab, cuerpo, pie);
    cerrar = abrirDialogo(document.getElementById("dialogo-admin"));
  }

  cargarMeseros();
  return {
    mostrar: cargar,
    /** Evento en vivo: se agrupan varios seguidos en una sola recarga. */
    actualizar() {
      clearTimeout(temporizador);
      temporizador = setTimeout(cargar, 300);
    },
  };
}
