import { PANTALLAS, sesion } from "./api.js";
import { crearBrasas, el, moneda, obtenerMenuPublico, reducirMovimiento, revelar, urlImagen } from "./ui.js";
import { dividirPalabras, iniciarEfectosDeScroll, inclinar } from "./efectos.js";

const MAX_DESTACADOS = 4;
const INTERVALO_CARRUSEL = 6500;
const MS_SALIDA_TARJETAS = 200; // debe coincidir con .producto.saliendo en landing.css

// ------------------------------------------------------------ cabecera / nav
const cabecera = document.getElementById("cabecera");
const botonMenu = document.getElementById("boton-menu");
const navegacion = document.getElementById("navegacion");

const alHacerScroll = () => cabecera.classList.toggle("con-fondo", window.scrollY > 12);
window.addEventListener("scroll", alHacerScroll, { passive: true });
alHacerScroll();

function alternarMenu(abierto) {
  botonMenu.setAttribute("aria-expanded", String(abierto));
  botonMenu.setAttribute("aria-label", abierto ? "Cerrar menú de navegación" : "Abrir menú de navegación");
  navegacion.classList.toggle("abierta", abierto);
}
botonMenu.addEventListener("click", () => alternarMenu(botonMenu.getAttribute("aria-expanded") !== "true"));
navegacion.addEventListener("click", (e) => { if (e.target.closest("a")) alternarMenu(false); });
document.addEventListener("keydown", (e) => { if (e.key === "Escape") alternarMenu(false); });

// Quien ya inició sesión ve un acceso directo a su pantalla en lugar de "Ingresar".
const activa = sesion();
if (activa && PANTALLAS[activa.usuario.rol]) {
  const enlace = document.getElementById("enlace-ingreso");
  enlace.textContent = "Mi pantalla";
  enlace.href = PANTALLAS[activa.usuario.rol];
}

document.getElementById("anio").textContent = new Date().getFullYear();
document.querySelectorAll(".titulo-palabras").forEach(dividirPalabras);
crearBrasas();
iniciarEfectosDeScroll();
revelar();

// -------------------------------------------------------------------- menú
const contenedorCategorias = document.getElementById("categorias");
const grilla = document.getElementById("grilla");
const campoBusqueda = document.getElementById("busqueda");

let categorias = [];
let categoriaActiva = "todas";
let textoBusqueda = "";
let versionVista = 0;

function emblema(clase) {
  const img = el("img", clase);
  img.src = "img/llama.svg";
  img.alt = "";
  return img;
}

function sinFoto() {
  const contenedor = el("div", "sin-foto");
  contenedor.append(emblema());
  return contenedor;
}

/** Foto (o emblema si no hay) para tarjetas y ventana de detalle. */
function crearFoto(producto, contenedor) {
  const ruta = urlImagen(producto.imagenUrl);
  if (!ruta) {
    contenedor.append(sinFoto());
    return;
  }
  const imagen = el("img", "real");
  imagen.alt = producto.nombre;
  imagen.loading = "lazy";
  imagen.addEventListener("load", () => imagen.classList.add("lista"));
  imagen.addEventListener("error", () => { imagen.remove(); contenedor.append(sinFoto()); });
  imagen.src = ruta;
  contenedor.append(imagen);
}

function mostrarEsqueletos() {
  grilla.replaceChildren(...Array.from({ length: 8 }, () => el("div", "esqueleto tarjeta-esqueleto")));
}

function crearTarjeta(producto, indice, nombreCategoria) {
  const tarjeta = el("article", "producto revelar");
  tarjeta.tabIndex = 0;
  tarjeta.setAttribute("role", "button");
  tarjeta.setAttribute("aria-label", `Ver detalle de ${producto.nombre}`);
  // La cascada se repite cada 4 tarjetas para que las filas largas no tarden en aparecer.
  tarjeta.style.setProperty("--retraso", `${(indice % 4) * 85}ms`);

  const foto = el("div", "foto");
  crearFoto(producto, foto);
  foto.append(el("span", "precio", moneda.format(producto.precio)));

  const info = el("div", "info");
  info.append(el("h3", "", producto.nombre));
  if (producto.descripcion) info.append(el("p", "", producto.descripcion));
  info.append(el("span", "ver-mas", "Ver detalle →"));

  tarjeta.append(foto, info);
  inclinar(tarjeta);
  const abrir = () => abrirDetalle(producto, nombreCategoria);
  tarjeta.addEventListener("click", abrir);
  tarjeta.addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") { e.preventDefault(); abrir(); }
  });
  return tarjeta;
}

function productosVisibles() {
  const consulta = textoBusqueda.trim().toLowerCase();
  const origen = categoriaActiva === "todas"
    ? categorias.flatMap((c) => c.productos.map((p) => ({ producto: p, categoria: c.nombre })))
    : (categorias.find((c) => String(c.id) === categoriaActiva)?.productos ?? [])
      .map((p) => ({ producto: p, categoria: categorias.find((c) => String(c.id) === categoriaActiva).nombre }));
  return consulta ? origen.filter((o) => o.producto.nombre.toLowerCase().includes(consulta)) : origen;
}

function construirGrilla() {
  const productos = productosVisibles();
  if (!productos.length) {
    const vacio = el("div", "estado-vacio");
    vacio.append(emblema(), el("p", "", textoBusqueda
      ? `No encontramos platos que coincidan con “${textoBusqueda}”.`
      : "Todavía no hay platos disponibles. Vuelve pronto."));
    grilla.replaceChildren(vacio);
    return;
  }
  grilla.replaceChildren(...productos.map((o, i) => crearTarjeta(o.producto, i, o.categoria)));
  revelar(grilla);
}

/** Las tarjetas actuales salen con una animación corta antes de mostrar las nuevas. */
function pintarGrilla() {
  const version = ++versionVista;
  const actuales = grilla.querySelectorAll(".producto");
  if (!actuales.length || reducirMovimiento) {
    construirGrilla();
    return;
  }
  actuales.forEach((t) => t.classList.add("saliendo"));
  setTimeout(() => { if (version === versionVista) construirGrilla(); }, MS_SALIDA_TARJETAS);
}

function pintarCategorias() {
  const opciones = [{ id: "todas", nombre: "Todo" }, ...categorias.map((c) => ({ id: String(c.id), nombre: c.nombre }))];
  contenedorCategorias.replaceChildren(...opciones.map((opcion) => {
    const chip = el("button", "chip", opcion.nombre);
    chip.type = "button";
    chip.setAttribute("aria-pressed", String(opcion.id === categoriaActiva));
    chip.addEventListener("click", () => {
      if (categoriaActiva === opcion.id) return;
      categoriaActiva = opcion.id;
      contenedorCategorias.querySelectorAll(".chip").forEach((c) => c.setAttribute("aria-pressed", String(c === chip)));
      chip.scrollIntoView({ inline: "center", block: "nearest", behavior: reducirMovimiento ? "auto" : "smooth" });
      pintarGrilla();
    });
    return chip;
  }));
}

let temporizadorBusqueda;
campoBusqueda.addEventListener("input", () => {
  clearTimeout(temporizadorBusqueda);
  temporizadorBusqueda = setTimeout(() => { textoBusqueda = campoBusqueda.value; pintarGrilla(); }, 220);
});

// ------------------------------------------------------- detalle del plato
const detalle = document.getElementById("detalle");
let cerrando = false;

function abrirDetalle(producto, nombreCategoria) {
  const foto = document.getElementById("detalle-foto");
  foto.replaceChildren();
  crearFoto(producto, foto);
  document.getElementById("detalle-categoria").textContent = nombreCategoria;
  document.getElementById("detalle-titulo").textContent = producto.nombre;
  document.getElementById("detalle-descripcion").textContent = producto.descripcion || "Un clásico de la casa, preparado al momento.";
  document.getElementById("detalle-precio").textContent = moneda.format(producto.precio);

  cerrando = false;
  detalle.showModal();
  requestAnimationFrame(() => requestAnimationFrame(() => detalle.classList.add("abierto")));
}

function cerrarDetalle() {
  if (cerrando || !detalle.open) return;
  cerrando = true;
  detalle.classList.remove("abierto");
  setTimeout(() => detalle.close(), reducirMovimiento ? 0 : 320);
}

document.getElementById("detalle-cerrar").addEventListener("click", cerrarDetalle);
// Clic fuera de la caja (sobre el fondo) o Esc cierran con animación.
detalle.addEventListener("click", (e) => { if (e.target === detalle) cerrarDetalle(); });
detalle.addEventListener("cancel", (e) => { e.preventDefault(); cerrarDetalle(); });
detalle.addEventListener("keydown", (e) => { if (e.key === "Escape") { e.preventDefault(); cerrarDetalle(); } });

// ---------------------------------------------------------------- carrusel
const contenedorDiapositivas = document.getElementById("diapositivas");
const precioDestacado = document.getElementById("precio-destacado");
const nombreDestacado = document.getElementById("nombre-destacado");
const controles = document.getElementById("carrusel-control");
const escenario = document.getElementById("escenario");

let diapositivas = [];
let destacados = [];
let posicion = 0;
let reloj;

function mostrarDiapositiva(i) {
  posicion = (i + destacados.length) % destacados.length;
  diapositivas.forEach((d, n) => d.classList.toggle("activa", n === posicion));
  const plato = destacados[posicion];
  nombreDestacado.textContent = plato.nombre;
  precioDestacado.textContent = moneda.format(plato.precio);
  document.getElementById("contador-actual").textContent = String(posicion + 1).padStart(2, "0");
}

function programarCarrusel() {
  clearInterval(reloj);
  if (reducirMovimiento || destacados.length < 2) return;
  reloj = setInterval(() => mostrarDiapositiva(posicion + 1), INTERVALO_CARRUSEL);
}

function iniciarCarrusel() {
  const todos = categorias.flatMap((c) => c.productos);
  // Se prefieren los platos con foto; si ninguno la tiene se muestra el emblema de la marca.
  destacados = [...todos.filter((p) => p.imagenUrl), ...todos.filter((p) => !p.imagenUrl)].slice(0, MAX_DESTACADOS);
  if (!destacados.length) {
    nombreDestacado.textContent = "Muy pronto, nuestra carta";
    const vacia = el("div", "diapositiva activa");
    vacia.append(emblema("emblema"));
    contenedorDiapositivas.replaceChildren(vacia);
    return;
  }

  diapositivas = destacados.map((plato) => {
    const d = el("div", "diapositiva");
    const ruta = urlImagen(plato.imagenUrl);
    if (ruta) {
      const imagen = el("img", "foto-plato");
      imagen.alt = plato.nombre;
      imagen.src = ruta;
      imagen.addEventListener("error", () => { imagen.replaceWith(emblema("emblema")); });
      d.append(imagen);
    } else {
      d.append(emblema("emblema"));
    }
    return d;
  });
  contenedorDiapositivas.replaceChildren(...diapositivas);
  precioDestacado.hidden = false;
  controles.hidden = destacados.length < 2;
  document.getElementById("contador-total").textContent = `/ ${String(destacados.length).padStart(2, "0")}`;
  mostrarDiapositiva(0);
  programarCarrusel();
}

document.getElementById("anterior").addEventListener("click", () => { mostrarDiapositiva(posicion - 1); programarCarrusel(); });
document.getElementById("siguiente").addEventListener("click", () => { mostrarDiapositiva(posicion + 1); programarCarrusel(); });
escenario.addEventListener("pointerenter", () => clearInterval(reloj));
escenario.addEventListener("pointerleave", programarCarrusel);
document.addEventListener("visibilitychange", () => (document.hidden ? clearInterval(reloj) : programarCarrusel()));

// ------------------------------------------------------------------- carga
async function cargarMenu() {
  mostrarEsqueletos();
  try {
    categorias = (await obtenerMenuPublico()).categorias;
    pintarCategorias();
    construirGrilla();
    iniciarCarrusel();
  } catch (e) {
    const error = el("div", "estado-vacio");
    const reintentar = el("button", "btn btn-fantasma", "Reintentar");
    reintentar.type = "button";
    reintentar.addEventListener("click", cargarMenu);
    error.append(emblema(), el("p", "", "No pudimos cargar el menú en este momento."), reintentar);
    grilla.replaceChildren(error);
    contenedorCategorias.replaceChildren();
  }
}

cargarMenu();
