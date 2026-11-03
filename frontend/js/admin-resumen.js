import { api } from "./api.js";
import { avisar, el, mensajeDeError, moneda, reducirMovimiento } from "./ui.js";
import { NOMBRE_ESTADO, NOMBRE_TIPO } from "./pedidos-comun.js";
import { esqueletos, estadoVacio, fechaLocal } from "./admin-ui.js";

const RANGOS = [
  { id: "hoy", nombre: "Hoy", desde: (h) => h },
  { id: "7", nombre: "7 días", desde: (h) => dias(h, -6) },
  { id: "30", nombre: "30 días", desde: (h) => dias(h, -29) },
  { id: "mes", nombre: "Este mes", desde: (h) => new Date(h.getFullYear(), h.getMonth(), 1) },
];
const MAX_DIAS_GRAFICO = 31;

function dias(fecha, delta) {
  const d = new Date(fecha);
  d.setDate(d.getDate() + delta);
  return d;
}

function formatoDuracion(segundos) {
  if (segundos === null || segundos === undefined) return "—";
  const total = Math.round(segundos);
  if (total < 60) return `${total} s`;
  const m = Math.floor(total / 60);
  const s = total % 60;
  return s ? `${m} min ${s} s` : `${m} min`;
}

/** Cuenta desde 0 hasta el valor final con una curva suave (o lo pone directo con "reducir movimiento"). */
function animarNumero(nodo, final, formato) {
  if (reducirMovimiento || final === 0) { nodo.textContent = formato(final); return; }
  const inicio = performance.now();
  const duracion = 900;
  function paso(ahora) {
    const t = Math.min(1, (ahora - inicio) / duracion);
    nodo.textContent = formato(final * (1 - Math.pow(1 - t, 3)));
    if (t < 1) requestAnimationFrame(paso);
  }
  requestAnimationFrame(paso);
  // Si la pestaña está en segundo plano el navegador pausa las animaciones: este respaldo asegura el valor final.
  setTimeout(() => { nodo.textContent = formato(final); }, duracion + 120);
}

const entero = (n) => String(Math.round(n));

export function iniciarResumen(raiz) {
  let rangoActivo = "30";
  let desde = fechaLocal(dias(new Date(), -29));
  let hasta = fechaLocal(new Date());
  let version = 0;

  // ------------------------------------------------------------ controles
  const cabecera = el("div", "vista-cabecera");
  const titulos = el("div");
  titulos.append(el("h1", "", "Resumen"), el("p", "suave", "Ventas, productos y operación del restaurante."));
  const controles = el("div", "rango");
  const chips = el("div", "categorias rango-chips");
  const fechas = el("div", "rango-fechas");
  const inputDesde = el("input", "control");
  const inputHasta = el("input", "control");
  inputDesde.type = inputHasta.type = "date";
  inputDesde.setAttribute("aria-label", "Desde");
  inputHasta.setAttribute("aria-label", "Hasta");
  fechas.append(inputDesde, el("span", "", "→"), inputHasta);
  controles.append(chips, fechas);
  cabecera.append(titulos, controles);

  const contenido = el("div", "resumen-contenido");
  raiz.append(cabecera, contenido);

  function pintarChips() {
    chips.replaceChildren(...RANGOS.map((r) => {
      const chip = el("button", "chip", r.nombre);
      chip.type = "button";
      chip.setAttribute("aria-pressed", String(r.id === rangoActivo));
      chip.addEventListener("click", () => {
        rangoActivo = r.id;
        desde = fechaLocal(r.desde(new Date()));
        hasta = fechaLocal(new Date());
        sincronizar();
        cargar();
      });
      return chip;
    }));
  }

  function sincronizar() {
    inputDesde.value = desde;
    inputHasta.value = hasta;
    inputDesde.max = inputHasta.max = fechaLocal(new Date());
    pintarChips();
  }

  function alCambiarFechas() {
    if (!inputDesde.value || !inputHasta.value) return;
    desde = inputDesde.value;
    hasta = inputHasta.value;
    rangoActivo = "";
    pintarChips();
    cargar();
  }
  inputDesde.addEventListener("change", alCambiarFechas);
  inputHasta.addEventListener("change", alCambiarFechas);

  // ----------------------------------------------------------- componentes
  function kpi(icono, etiqueta, valor, formato, nota, clase = "") {
    const tarjeta = el("article", `kpi ${clase}`);
    tarjeta.append(el("span", "kpi-icono", icono));
    const texto = el("div");
    texto.append(el("span", "kpi-etiqueta", etiqueta));
    const numero = el("strong", "kpi-valor", formato(0));
    texto.append(numero);
    if (nota) texto.append(el("small", "kpi-nota", nota));
    tarjeta.append(texto);
    if (valor !== null) animarNumero(numero, valor, formato); else numero.textContent = "—";
    return tarjeta;
  }

  function panel(titulo, subtitulo) {
    const p = el("section", "panel-tarjeta");
    const encabezado = el("div", "panel-encabezado");
    encabezado.append(el("h2", "", titulo));
    if (subtitulo) encabezado.append(el("small", "suave", subtitulo));
    p.append(encabezado);
    return p;
  }

  function barrasPorDia(porDia) {
    const mapa = new Map(porDia.map((d) => [d.fecha, d]));
    let lista = porDia;
    // Con rangos cortos se muestran también los días sin ventas, para que la escala de tiempo sea continua.
    const rango = Math.round((new Date(`${hasta}T00:00`) - new Date(`${desde}T00:00`)) / 86400000) + 1;
    if (rango <= MAX_DIAS_GRAFICO) {
      lista = Array.from({ length: rango }, (_, i) => {
        const f = fechaLocal(dias(new Date(`${desde}T00:00`), i));
        return mapa.get(f) ?? { fecha: f, pedidos: 0, total: 0 };
      });
    }
    const maximo = Math.max(...lista.map((d) => Number(d.total)), 1);
    const grafico = el("div", "grafico-barras");
    grafico.setAttribute("role", "img");
    grafico.setAttribute("aria-label", `Ventas por día: ${lista.map((d) => `${d.fecha} ${moneda.format(d.total)}`).join(", ")}`);
    const salto = Math.ceil(lista.length / 10);
    lista.forEach((d, i) => {
      const columna = el("div", "columna-barra");
      columna.title = `${new Date(`${d.fecha}T00:00`).toLocaleDateString("es-CO", { weekday: "short", day: "numeric", month: "short" })}: ${moneda.format(d.total)} · ${d.pedidos} pedido${d.pedidos === 1 ? "" : "s"}`;
      const barra = el("div", "barra-valor");
      barra.style.setProperty("--alto", `${Math.max((Number(d.total) / maximo) * 100, d.total > 0 ? 3 : 0)}%`);
      barra.style.setProperty("--retraso", `${i * 25}ms`);
      columna.append(barra);
      const etiqueta = el("span", "barra-etiqueta", i % salto === 0 ? d.fecha.slice(8) + "/" + d.fecha.slice(5, 7) : "");
      columna.append(etiqueta);
      grafico.append(columna);
    });
    setTimeout(() => grafico.classList.add("listo"), 40);
    return grafico;
  }

  function listaProporcional(filas, formatoValor) {
    const maximo = Math.max(...filas.map((f) => f.valor), 1);
    const lista = el("ul", "lista-barras escalonado");
    filas.forEach((f, i) => {
      const li = el("li");
      const cabecera = el("div", "lista-barras-linea");
      cabecera.append(el("span", "lista-barras-nombre", f.nombre), el("strong", "", formatoValor(f)));
      const pista = el("div", "pista");
      const relleno = el("div", "relleno");
      relleno.style.setProperty("--ancho", `${(f.valor / maximo) * 100}%`);
      relleno.style.setProperty("--retraso", `${i * 70}ms`);
      if (f.clase) relleno.classList.add(f.clase);
      pista.append(relleno);
      li.append(cabecera, pista);
      if (f.nota) li.append(el("small", "suave", f.nota));
      lista.append(li);
    });
    setTimeout(() => lista.classList.add("listo"), 40);
    return lista;
  }

  // ------------------------------------------------------------------ vista
  function pintar({ ventas, productos, cancelaciones, tiempos }) {
    const rejilla = el("div", "kpis");
    rejilla.append(
      kpi("💰", "Ventas", Number(ventas.totalVentas), (n) => moneda.format(n), `${ventas.pedidos} pedidos entregados`, "destacado"),
      kpi("🧾", "Ticket promedio", Number(ventas.ticketPromedio), (n) => moneda.format(n), "por pedido entregado"),
      kpi("🛵", "Envíos cobrados", Number(ventas.totalEnvios), (n) => moneda.format(n), "incluidos en las ventas"),
      kpi("⏱️", "Preparación promedio", tiempos.promedioSegundos ?? null, formatoDuracion, tiempos.pedidos ? `${tiempos.pedidos} pedidos medidos` : "Sin datos en el rango"),
    );

    const grafico = panel("Ventas por día", `${desde} → ${hasta}`);
    grafico.classList.add("ancho-doble");
    grafico.append(ventas.porDia.length || ventas.pedidos ? barrasPorDia(ventas.porDia) : estadoVacio("Aún no hay ventas en este rango."));

    const porTipo = panel("Ventas por tipo de pedido");
    const totalVentas = Number(ventas.totalVentas) || 1;
    porTipo.append(ventas.porTipo.length
      ? listaProporcional(
        ventas.porTipo.map((t) => ({ nombre: NOMBRE_TIPO[t.tipo] ?? t.tipo, valor: Number(t.total), nota: `${t.pedidos} pedido${t.pedidos === 1 ? "" : "s"} · ${Math.round((Number(t.total) / totalVentas) * 100)} %` })),
        (f) => moneda.format(f.valor))
      : estadoVacio("Sin datos."));

    const top = panel("Productos más vendidos", "por unidades");
    top.append(productos.productos.length
      ? listaProporcional(
        productos.productos.map((p) => ({ nombre: p.nombre, valor: p.unidades, nota: moneda.format(p.ingresos) })),
        (f) => `${f.valor} uds.`)
      : estadoVacio("Sin ventas en este rango."));

    const canc = panel("Cancelaciones", `${cancelaciones.total} pedido${cancelaciones.total === 1 ? "" : "s"} · ${moneda.format(cancelaciones.valorCancelado)}`);
    if (!cancelaciones.total) {
      canc.append(estadoVacio("Ninguna cancelación. ¡Buen trabajo!"));
    } else {
      const bloque = el("div", "doble-columna");
      const porEstado = el("div");
      porEstado.append(el("h3", "subtitulo", "Dónde se cancelaron"));
      porEstado.append(listaProporcional(
        cancelaciones.porEstadoPrevio.map((c) => ({ nombre: NOMBRE_ESTADO[c.estadoPrevio] ?? c.estadoPrevio, valor: c.cancelaciones, clase: "rojo" })),
        (f) => String(f.valor)));
      const porMesero = el("div");
      porMesero.append(el("h3", "subtitulo", "Por mesero"));
      porMesero.append(listaProporcional(
        cancelaciones.porMesero.map((c) => ({ nombre: c.mesero, valor: c.cancelaciones, nota: moneda.format(c.valor), clase: "rojo" })),
        (f) => String(f.valor)));
      bloque.append(porEstado, porMesero);
      canc.append(bloque);
    }

    const prep = panel("Tiempo de preparación", "de EN_PREPARACION a LISTO");
    if (!tiempos.pedidos) {
      prep.append(estadoVacio("Sin pedidos preparados en este rango."));
    } else {
      const mini = el("div", "mini-kpis");
      [["Mínimo", tiempos.minimoSegundos], ["Promedio", tiempos.promedioSegundos], ["90 % en menos de", tiempos.p90Segundos], ["Máximo", tiempos.maximoSegundos]].forEach(([n, v]) => {
        const c = el("div", "mini-kpi");
        c.append(el("span", "", n), el("strong", "", formatoDuracion(v)));
        mini.append(c);
      });
      prep.append(mini);
      prep.append(listaProporcional(
        tiempos.porTipo.map((t) => ({ nombre: NOMBRE_TIPO[t.tipo] ?? t.tipo, valor: t.promedioSegundos, nota: `${t.pedidos} pedido${t.pedidos === 1 ? "" : "s"}`, clase: "morado" })),
        (f) => formatoDuracion(f.valor)));
    }

    const paneles = el("div", "paneles");
    paneles.append(grafico, porTipo, top, canc, prep);
    contenido.replaceChildren(rejilla, paneles);
  }

  async function cargar() {
    const miVersion = ++version;
    contenido.replaceChildren(esqueletos(4, "92px"), esqueletos(2, "260px"));
    const q = `desde=${desde}&hasta=${hasta}`;
    try {
      const [ventas, productos, cancelaciones, tiempos] = await Promise.all([
        api("GET", `/api/admin/reportes/ventas?${q}`),
        api("GET", `/api/admin/reportes/productos-mas-vendidos?${q}&limite=8`),
        api("GET", `/api/admin/reportes/cancelaciones?${q}`),
        api("GET", `/api/admin/reportes/tiempo-preparacion?${q}`),
      ]);
      if (miVersion === version) pintar({ ventas, productos, cancelaciones, tiempos });
    } catch (e) {
      if (miVersion !== version) return;
      const reintentar = el("button", "btn btn-fantasma", "Reintentar");
      reintentar.type = "button";
      reintentar.addEventListener("click", cargar);
      contenido.replaceChildren(estadoVacio(mensajeDeError(e), reintentar));
      avisar(mensajeDeError(e), "error");
    }
  }

  sincronizar();
  return { mostrar: cargar, actualizar: cargar };
}
