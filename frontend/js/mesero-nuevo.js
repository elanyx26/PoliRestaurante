import { ApiError, api } from "./api.js";
import { avisar, crearPaso, el, mensajeDeError, moneda, reducirMovimiento, revelar, urlImagen } from "./ui.js";
import { obtenerMenu } from "./menu-datos.js";

const $ = (id) => document.getElementById(id);
const MAX_CANTIDAD = 100;
const ANCHO_MOVIL = 980;

export function iniciarNuevoPedido({ alCrear }) {
  const carrito = new Map(); // productoId -> { producto, cantidad, notas }
  const acciones = new Map(); // productoId -> contenedor de la tarjeta del menú
  const lineas = new Map(); // productoId -> { nodo, paso, valor }

  let menu = { categorias: [], productos: [] };
  let categoriaActiva = "todas";
  let busqueda = "";
  let tipo = "SALON";
  let pago = "EFECTIVO_CONTRAENTREGA";
  let mesaId = null;

  const grilla = $("menu-grilla");
  const chips = $("menu-chips");
  const panelCarrito = $("carrito");
  const velo = $("carrito-velo");
  const fab = $("fab-carrito");
  const aviso = $("aviso-pedido");
  const botonEnviar = $("btn-enviar");

  // ---------------------------------------------------------------- menú
  function productosFiltrados() {
    const consulta = busqueda.trim().toLowerCase();
    return menu.productos.filter((p) =>
      (categoriaActiva === "todas" || String(p.categoriaId) === categoriaActiva)
      && (!consulta || p.nombre.toLowerCase().includes(consulta)));
  }

  function pintarChips() {
    const opciones = [{ id: "todas", nombre: "Todo" }, ...menu.categorias.map((c) => ({ id: String(c.id), nombre: c.nombre }))];
    chips.replaceChildren(...opciones.map((o) => {
      const chip = el("button", "chip", o.nombre);
      chip.type = "button";
      chip.setAttribute("aria-pressed", String(o.id === categoriaActiva));
      chip.addEventListener("click", () => {
        categoriaActiva = o.id;
        chips.querySelectorAll(".chip").forEach((c) => c.setAttribute("aria-pressed", String(c === chip)));
        pintarMenu();
      });
      return chip;
    }));
  }

  function crearFoto(producto) {
    const foto = el("div", "foto");
    const ruta = urlImagen(producto.imagenUrl);
    if (ruta) {
      const imagen = el("img", "real");
      imagen.alt = "";
      imagen.loading = "lazy";
      imagen.src = ruta;
      imagen.addEventListener("error", () => { imagen.remove(); foto.prepend(emblema()); });
      foto.append(imagen);
    } else {
      foto.append(emblema());
    }
    foto.append(el("span", "precio", moneda.format(producto.precio)));
    return foto;
  }

  function emblema() {
    const contenedor = el("div", "sin-foto");
    const img = el("img");
    img.src = "img/llama.svg";
    img.alt = "";
    contenedor.append(img);
    return contenedor;
  }

  function pintarAccion(producto) {
    const contenedor = acciones.get(producto.id);
    if (!contenedor) return;
    contenedor.replaceChildren();
    const item = carrito.get(producto.id);
    contenedor.closest(".plato").classList.toggle("en-pedido", Boolean(item));

    if (!item) {
      const boton = el("button", "btn btn-primario", "Agregar");
      boton.type = "button";
      boton.addEventListener("click", () => agregar(producto, boton));
      contenedor.append(boton);
      return;
    }
    const paso = crearPaso(item.cantidad, {
      minimo: 0,
      alRestar: () => cambiarCantidad(producto.id, -1),
      alSumar: () => cambiarCantidad(producto.id, +1),
    });
    paso.fijar(item.cantidad);
    contenedor.append(paso.nodo, el("span", "subtotal", moneda.format(item.cantidad * producto.precio)));
  }

  function crearPlato(producto, indice) {
    const tarjeta = el("article", "plato revelar");
    tarjeta.style.setProperty("--retraso", `${(indice % 4) * 70}ms`);
    const info = el("div", "info");
    info.append(el("h3", "", producto.nombre));
    if (producto.descripcion) info.append(el("p", "", producto.descripcion));
    const accion = el("div", "accion");
    acciones.set(producto.id, accion);
    tarjeta.append(crearFoto(producto), info, accion);
    return tarjeta;
  }

  function pintarMenu() {
    acciones.clear();
    const lista = productosFiltrados();
    if (!lista.length) {
      grilla.replaceChildren(el("p", "vacio", busqueda ? `Sin resultados para “${busqueda}”.` : "No hay platos disponibles."));
      return;
    }
    grilla.replaceChildren(...lista.map(crearPlato));
    lista.forEach(pintarAccion);
    revelar(grilla);
  }

  let temporizadorBusqueda;
  $("menu-buscar").addEventListener("input", (e) => {
    clearTimeout(temporizadorBusqueda);
    temporizadorBusqueda = setTimeout(() => { busqueda = e.target.value; pintarMenu(); }, 200);
  });

  // -------------------------------------------------------------- carrito
  const destinoVuelo = () => (window.innerWidth <= ANCHO_MOVIL ? fab : $("contador-items"));

  function rebotar(elemento, clase) {
    elemento.classList.remove(clase);
    void elemento.offsetWidth;
    elemento.classList.add(clase);
    setTimeout(() => elemento.classList.remove(clase), 520);
  }

  /** Una miniatura del plato "vuela" desde la tarjeta hasta el carrito. */
  function volarAlCarrito(origen, producto) {
    if (reducirMovimiento) return;
    const destino = destinoVuelo();
    if (!destino) return;
    const o = origen.getBoundingClientRect();
    const d = destino.getBoundingClientRect();
    const volador = el("div", "volador");
    const ruta = urlImagen(producto.imagenUrl);
    if (ruta) {
      const img = el("img");
      img.src = ruta;
      img.alt = "";
      volador.append(img);
    }
    volador.style.left = `${o.left + o.width / 2 - 23}px`;
    volador.style.top = `${o.top + o.height / 2 - 23}px`;
    document.body.append(volador);
    const dx = d.left + d.width / 2 - (o.left + o.width / 2);
    const dy = d.top + d.height / 2 - (o.top + o.height / 2);
    volador.animate([
      { transform: "translate(0, 0) scale(1)", opacity: 1 },
      { transform: `translate(${dx * 0.55}px, ${dy * 0.55 - 90}px) scale(0.85)`, opacity: 1, offset: 0.55 },
      { transform: `translate(${dx}px, ${dy}px) scale(0.25)`, opacity: 0.5 },
    ], { duration: 700, easing: "cubic-bezier(0.4, 0.1, 0.3, 1)" }).finished.then(() => {
      volador.remove();
      if (destino === fab) rebotar(fab, "rebote");
    }).catch(() => volador.remove());
  }

  function agregar(producto, origen) {
    carrito.set(producto.id, { producto, cantidad: 1, notas: "" });
    volarAlCarrito(origen, producto);
    agregarLinea(carrito.get(producto.id));
    pintarAccion(producto);
    actualizarTotales();
  }

  function cambiarCantidad(id, delta) {
    const item = carrito.get(id);
    if (!item) return;
    const nueva = item.cantidad + delta;
    if (nueva <= 0) return quitar(id);
    if (nueva > MAX_CANTIDAD) return avisar(`Máximo ${MAX_CANTIDAD} unidades por producto`, "error");
    item.cantidad = nueva;
    const linea = lineas.get(id);
    linea.paso.fijar(nueva);
    linea.valor.textContent = `${nueva} × ${moneda.format(item.producto.precio)} = ${moneda.format(nueva * item.producto.precio)}`;
    pintarAccion(item.producto);
    actualizarTotales();
  }

  function quitar(id) {
    const item = carrito.get(id);
    const linea = lineas.get(id);
    carrito.delete(id);
    lineas.delete(id);
    if (item) pintarAccion(item.producto);
    if (linea) {
      linea.nodo.classList.add("saliendo");
      setTimeout(() => { linea.nodo.remove(); mostrarVacio(); }, reducirMovimiento ? 0 : 230);
    }
    actualizarTotales();
  }

  function agregarLinea(item) {
    $("carrito-items").querySelector(".carrito-vacio")?.remove();
    const nodo = el("div", "linea");
    const encabezado = el("div");
    encabezado.append(el("div", "nombre", item.producto.nombre));
    const valor = el("div", "valor", `1 × ${moneda.format(item.producto.precio)} = ${moneda.format(item.producto.precio)}`);
    encabezado.append(valor);

    const paso = crearPaso(item.cantidad, {
      minimo: 0,
      alRestar: () => cambiarCantidad(item.producto.id, -1),
      alSumar: () => cambiarCantidad(item.producto.id, +1),
    });
    paso.fijar(item.cantidad);

    const nota = el("input", "nota");
    nota.type = "text";
    nota.maxLength = 255;
    nota.placeholder = "Nota (ej. sin cebolla)";
    nota.setAttribute("aria-label", `Nota para ${item.producto.nombre}`);
    nota.addEventListener("input", () => { item.notas = nota.value; });

    const quitarBoton = el("button", "quitar", "Quitar");
    quitarBoton.type = "button";
    quitarBoton.addEventListener("click", () => quitar(item.producto.id));

    const derecha = el("div");
    derecha.style.cssText = "display:grid;justify-items:end;gap:.3rem";
    derecha.append(paso.nodo, quitarBoton);
    nodo.append(encabezado, derecha, nota);
    $("carrito-items").append(nodo);
    lineas.set(item.producto.id, { nodo, paso, valor });
  }

  function mostrarVacio() {
    const cont = $("carrito-items");
    if (!carrito.size && !cont.querySelector(".carrito-vacio")) {
      cont.replaceChildren(el("p", "carrito-vacio", "Agrega platos desde el menú."));
    }
  }

  function subtotal() {
    let suma = 0;
    carrito.forEach((i) => { suma += i.cantidad * i.producto.precio; });
    return suma;
  }
  const envio = () => (tipo === "DOMICILIO" ? Math.max(0, Number($("envio").value) || 0) : 0);

  function actualizarTotales() {
    const unidades = [...carrito.values()].reduce((n, i) => n + i.cantidad, 0);
    const total = subtotal() + envio();
    $("total-subtotal").textContent = moneda.format(subtotal());
    $("total-envio").textContent = moneda.format(envio());
    $("fila-envio").hidden = tipo !== "DOMICILIO";
    const final = $("total-final");
    const anterior = final.textContent;
    final.textContent = moneda.format(total);
    if (anterior !== final.textContent) rebotar(final, "cambio");
    $("contador-items").textContent = String(unidades);
    $("fab-cuenta").textContent = String(unidades);
    $("fab-total").textContent = moneda.format(total);
    mostrarAviso("");
  }
  $("envio").addEventListener("input", actualizarTotales);

  // ------------------------------------------------------------ tipo y mesa
  function elegirTipo(nuevo) {
    tipo = nuevo;
    document.querySelectorAll(".tipos:not(.pagos) .tipo").forEach((b) => b.setAttribute("aria-pressed", String(b.dataset.tipo === tipo)));
    $("campo-mesa").hidden = tipo !== "SALON";
    $("campo-cliente").hidden = tipo === "SALON";
    $("campo-entrega").hidden = tipo !== "DOMICILIO";
    actualizarTotales();
  }
  document.querySelectorAll(".tipos:not(.pagos) .tipo").forEach((b) => b.addEventListener("click", () => elegirTipo(b.dataset.tipo)));

  document.querySelectorAll(".pagos .tipo").forEach((b) => b.addEventListener("click", () => {
    pago = b.dataset.pago;
    document.querySelectorAll(".pagos .tipo").forEach((x) => x.setAttribute("aria-pressed", String(x === b)));
  }));

  function pintarMesas(mesas) {
    const lista = $("mesas-lista");
    if (!mesas.length) {
      lista.replaceChildren(el("p", "sin-mesas", "No hay mesas libres en este momento."));
      return;
    }
    lista.replaceChildren(...mesas.map((m, i) => {
      const boton = el("button", "mesa", String(m.numero));
      boton.type = "button";
      boton.style.animationDelay = `${i * 30}ms`;
      boton.append(el("small", "", `${m.capacidad} pers.`));
      boton.setAttribute("aria-pressed", String(m.id === mesaId));
      boton.setAttribute("aria-label", `Mesa ${m.numero}, ${m.capacidad} personas`);
      boton.addEventListener("click", () => {
        mesaId = m.id;
        lista.querySelectorAll(".mesa").forEach((x) => x.setAttribute("aria-pressed", String(x === boton)));
      });
      return boton;
    }));
  }

  async function cargarMesas() {
    try {
      const mesas = await api("GET", "/api/mesas/disponibles");
      if (mesaId && !mesas.some((m) => m.id === mesaId)) mesaId = null; // otra persona la ocupó
      pintarMesas(mesas);
    } catch (e) {
      avisar(e.message, "error");
    }
  }

  // ------------------------------------------------------------------ envío
  function mostrarAviso(texto) {
    aviso.textContent = texto;
    aviso.hidden = !texto;
  }

  function validar() {
    if (!carrito.size) return "Agrega al menos un producto.";
    if (tipo === "SALON" && !mesaId) return "Elige una mesa libre.";
    if (tipo !== "SALON" && !$("cliente").value.trim()) return "Escribe el nombre del cliente.";
    if (tipo === "DOMICILIO") {
      if (!$("direccion").value.trim()) return "Escribe la dirección de entrega.";
      if (!$("telefono").value.trim()) return "Escribe el teléfono del cliente.";
    }
    return null;
  }

  function construirCuerpo() {
    const cuerpo = {
      tipo,
      items: [...carrito.values()].map((i) => ({
        productoId: i.producto.id,
        cantidad: i.cantidad,
        notas: i.notas.trim() || null,
      })),
    };
    if (tipo === "SALON") cuerpo.mesaId = mesaId;
    if (tipo !== "SALON") cuerpo.nombreCliente = $("cliente").value.trim();
    if (tipo === "DOMICILIO") {
      cuerpo.entrega = {
        direccion: $("direccion").value.trim(),
        telefono: $("telefono").value.trim(),
        costoEnvio: envio(),
        metodoPago: pago,
      };
    }
    return cuerpo;
  }

  function limpiar() {
    [...carrito.keys()].forEach((id) => { const i = carrito.get(id); carrito.delete(id); lineas.delete(id); pintarAccion(i.producto); });
    $("carrito-items").replaceChildren();
    mostrarVacio();
    mesaId = null;
    ["cliente", "direccion", "telefono"].forEach((id) => { $(id).value = ""; });
    $("envio").value = "0";
    actualizarTotales();
  }

  botonEnviar.addEventListener("click", async () => {
    const problema = validar();
    if (problema) {
      mostrarAviso(problema);
      return;
    }
    mostrarAviso("");
    botonEnviar.classList.add("cargando");
    try {
      const pedido = await api("POST", "/api/pedidos", construirCuerpo());
      limpiar();
      cerrarCarrito();
      await cargarMesas();
      alCrear(pedido);
    } catch (e) {
      mostrarAviso(mensajeDeError(e));
      if (e instanceof ApiError && e.estado === 409) cargarMesas();
    } finally {
      botonEnviar.classList.remove("cargando");
    }
  });

  // ------------------------------------------- hoja del carrito en móvil
  function abrirCarrito() {
    panelCarrito.classList.add("abierto");
    velo.hidden = false;
    requestAnimationFrame(() => velo.classList.add("visible"));
    fab.classList.add("oculto");
  }
  function cerrarCarrito() {
    panelCarrito.classList.remove("abierto");
    velo.classList.remove("visible");
    fab.classList.remove("oculto");
    setTimeout(() => { velo.hidden = true; }, 320);
  }
  fab.addEventListener("click", abrirCarrito);
  velo.addEventListener("click", cerrarCarrito);
  $("cerrar-carrito").addEventListener("click", cerrarCarrito);
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && panelCarrito.classList.contains("abierto")) cerrarCarrito(); });

  // ------------------------------------------------------------------ inicio
  mostrarVacio();
  elegirTipo("SALON");
  grilla.replaceChildren(...Array.from({ length: 8 }, () => el("div", "esqueleto", "")).map((e) => { e.style.height = "250px"; return e; }));

  obtenerMenu()
    .then((datos) => { menu = datos; pintarChips(); pintarMenu(); })
    .catch((e) => grilla.replaceChildren(el("p", "vacio", e.message)));
  cargarMesas();

  return {
    recargarMesas: cargarMesas,
    /** Productos del menú, para el selector de "agregar producto" de un pedido existente. */
    productos: () => menu.productos,
    mostrarBotonFlotante(visible) { fab.hidden = !visible; },
  };
}
