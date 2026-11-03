import { ApiError } from "./api.js";
import { abrirDialogo, avisar, el, mensajeDeError } from "./ui.js";

const $ = (id) => document.getElementById(id);

export const fechaLocal = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

export const fechaLarga = (iso) => new Date(iso).toLocaleString("es-CO", { day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });

// ------------------------------------------------------------- interruptor
/** Interruptor accesible. `alCambiar(nuevo)` debe devolver una promesa; si falla, vuelve al valor anterior. */
export function crearInterruptor({ activo, etiqueta, alCambiar }) {
  const boton = el("button", "interruptor");
  boton.type = "button";
  boton.setAttribute("role", "switch");
  boton.setAttribute("aria-label", etiqueta);
  boton.append(el("span", "interruptor-bola"));
  const pintar = (valor) => { boton.setAttribute("aria-checked", String(valor)); boton.classList.toggle("encendido", valor); };
  pintar(activo);

  boton.addEventListener("click", async () => {
    const nuevo = boton.getAttribute("aria-checked") !== "true";
    pintar(nuevo);
    boton.disabled = true;
    try {
      await alCambiar(nuevo);
    } catch (e) {
      pintar(!nuevo);
      // Una cancelación del propio usuario (por ejemplo en un diálogo de confirmación) no es un error que mostrar.
      if (!e?.cancelado) avisar(mensajeDeError(e), "error");
    } finally {
      boton.disabled = false;
    }
  });
  return boton;
}

// ------------------------------------------------------------------ tabla
/** columnas: [{ titulo, render(fila) -> Node|string, clase }]. En móvil cada fila se convierte en una tarjeta. */
export function crearTabla(columnas, filas, { alClicFila } = {}) {
  const tabla = el("table", "tabla");
  const cabecera = el("thead");
  const filaCab = el("tr");
  columnas.forEach((c) => filaCab.append(el("th", c.clase ?? "", c.titulo)));
  cabecera.append(filaCab);

  const cuerpo = el("tbody", "escalonado");
  filas.forEach((fila) => {
    const tr = el("tr");
    columnas.forEach((c) => {
      const td = el("td", c.clase ?? "");
      td.dataset.titulo = c.titulo;
      const contenido = c.render(fila);
      if (contenido instanceof Node) td.append(contenido); else td.textContent = String(contenido ?? "");
      tr.append(td);
    });
    if (alClicFila) {
      tr.classList.add("clicable");
      tr.tabIndex = 0;
      tr.addEventListener("click", (e) => { if (!e.target.closest("button, a, input, select, label")) alClicFila(fila); });
      tr.addEventListener("keydown", (e) => { if (e.key === "Enter" && e.target === tr) alClicFila(fila); });
    }
    cuerpo.append(tr);
  });
  tabla.append(cabecera, cuerpo);
  const envoltura = el("div", "tabla-envoltura");
  envoltura.append(tabla);
  return envoltura;
}

export function crearPaginacion({ pagina, totalPaginas, total, alCambiar }) {
  const nodo = el("div", "paginacion");
  nodo.append(el("span", "paginacion-texto", total === 0 ? "Sin resultados" : `Página ${pagina + 1} de ${Math.max(totalPaginas, 1)} · ${total} resultado${total === 1 ? "" : "s"}`));
  const botones = el("div", "paginacion-botones");
  const anterior = el("button", "btn btn-suave", "← Anterior");
  const siguiente = el("button", "btn btn-suave", "Siguiente →");
  anterior.type = siguiente.type = "button";
  anterior.disabled = pagina <= 0;
  siguiente.disabled = pagina + 1 >= totalPaginas;
  anterior.addEventListener("click", () => alCambiar(pagina - 1));
  siguiente.addEventListener("click", () => alCambiar(pagina + 1));
  botones.append(anterior, siguiente);
  nodo.append(botones);
  return nodo;
}

export function estadoVacio(texto, accion) {
  const caja = el("div", "estado-vacio-admin");
  const img = el("img");
  img.src = "img/llama.svg";
  img.alt = "";
  caja.append(img, el("p", "", texto));
  if (accion) caja.append(accion);
  return caja;
}

export function esqueletos(cantidad, alto = "74px") {
  const caja = el("div", "esqueletos");
  for (let i = 0; i < cantidad; i++) {
    const e = el("div", "esqueleto");
    e.style.height = alto;
    caja.append(e);
  }
  return caja;
}

// ------------------------------------------------------------- formularios
/**
 * Abre un formulario en el diálogo compartido.
 * campos: [{ nombre, etiqueta, tipo: text|email|password|number|textarea|select|checkbox, opciones, requerido, min, max, step, minLength, maxLength, ayuda, placeholder }]
 * `alGuardar(valores)` debe lanzar si falla; los errores de validación del servidor se muestran junto a cada campo.
 */
export function abrirFormulario({ titulo, subtitulo, campos, valores = {}, textoGuardar = "Guardar", extra, alGuardar }) {
  const dialogo = $("dialogo-admin");
  const caja = $("dialogo-admin-contenido");
  let cerrar = () => {};

  const cabecera = el("div", "dialogo-cabecera");
  const h2 = el("h2", "", titulo);
  h2.id = "dialogo-admin-titulo";
  const x = el("button", "cerrar-x", "✕");
  x.type = "button";
  x.setAttribute("aria-label", "Cerrar");
  x.addEventListener("click", () => cerrar());
  cabecera.append(h2, x);

  const formulario = el("form", "formulario");
  formulario.noValidate = false;
  const cuerpo = el("div", "dialogo-cuerpo");
  if (subtitulo) cuerpo.append(el("p", "aviso info", subtitulo));

  const controles = new Map();
  const mensajes = new Map();
  const dispuestos = el("div", "campos-rejilla");

  for (const c of campos) {
    const valor = valores[c.nombre];
    let control;
    let envoltura;
    if (c.tipo === "checkbox") {
      envoltura = el("label", "campo-casilla");
      control = el("input");
      control.type = "checkbox";
      control.checked = Boolean(valor ?? c.porDefecto ?? false);
      envoltura.append(control, el("span", "", c.etiqueta));
    } else {
      envoltura = el("label", "campo");
      envoltura.append(el("span", "", c.etiqueta + (c.requerido ? " *" : "")));
      if (c.tipo === "select") {
        control = el("select", "control");
        c.opciones.forEach((o) => control.append(new Option(o.texto, String(o.valor))));
        control.value = String(valor ?? c.opciones[0]?.valor ?? "");
      } else if (c.tipo === "textarea") {
        control = el("textarea", "control");
        control.rows = 3;
        control.value = valor ?? "";
      } else {
        control = el("input", "control");
        control.type = c.tipo ?? "text";
        control.value = valor ?? "";
        if (c.tipo === "password") control.autocomplete = "new-password";
      }
      if (c.placeholder) control.placeholder = c.placeholder;
      envoltura.append(control);
    }
    control.name = c.nombre;
    if (c.requerido) control.required = true;
    ["min", "max", "step", "minLength", "maxLength"].forEach((p) => { if (c[p] !== undefined) control[p] = c[p]; });
    if (c.ayuda) envoltura.append(el("small", "ayuda", c.ayuda));
    const mensaje = el("small", "error-campo");
    mensaje.hidden = true;
    envoltura.append(mensaje);
    if (c.ancho === "completo") envoltura.classList.add("completo");
    controles.set(c.nombre, control);
    mensajes.set(c.nombre, mensaje);
    dispuestos.append(envoltura);
  }
  cuerpo.append(dispuestos);
  if (extra) cuerpo.append(extra);
  const aviso = el("p", "aviso error", "");
  aviso.hidden = true;
  aviso.setAttribute("role", "alert");
  cuerpo.append(aviso);

  const pie = el("div", "dialogo-pie");
  const cancelar = el("button", "btn btn-suave", "Cancelar");
  cancelar.type = "button";
  cancelar.addEventListener("click", () => cerrar());
  const guardar = el("button", "btn btn-primario", textoGuardar);
  guardar.type = "submit";
  pie.append(cancelar, guardar);
  formulario.append(cuerpo, pie);
  caja.replaceChildren(cabecera, formulario);

  function leerValores() {
    const salida = {};
    for (const c of campos) {
      const control = controles.get(c.nombre);
      salida[c.nombre] = c.tipo === "checkbox" ? control.checked : c.tipo === "number" ? (control.value === "" ? null : Number(control.value)) : control.value.trim();
    }
    return salida;
  }

  function limpiarErrores() {
    aviso.hidden = true;
    mensajes.forEach((m) => { m.hidden = true; m.textContent = ""; });
    controles.forEach((c) => c.classList.remove("invalido"));
  }

  formulario.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    limpiarErrores();
    guardar.classList.add("cargando");
    try {
      await alGuardar(leerValores());
      cerrar();
    } catch (e) {
      const porCampo = e instanceof ApiError ? e.errores : null;
      if (porCampo) {
        for (const [campo, texto] of Object.entries(porCampo)) {
          const m = mensajes.get(campo);
          if (m) { m.textContent = texto; m.hidden = false; controles.get(campo).classList.add("invalido"); }
        }
      }
      if (!porCampo || !Object.keys(porCampo).some((k) => mensajes.has(k))) {
        aviso.textContent = mensajeDeError(e);
        aviso.hidden = false;
      }
      caja.querySelector(".invalido")?.focus();
    } finally {
      guardar.classList.remove("cargando");
    }
  });

  cerrar = abrirDialogo(dialogo);
  setTimeout(() => dispuestos.querySelector("input:not([type=checkbox]), select, textarea")?.focus(), 350);
}

// ------------------------------------------------------------ confirmación
export function confirmar({ titulo, mensaje, textoConfirmar = "Confirmar", peligro = false }) {
  return new Promise((resolver) => {
    const dialogo = $("dialogo-confirmar");
    const caja = $("dialogo-confirmar-contenido");
    let decidido = false;
    let cerrar = () => {};
    const decidir = (valor) => {
      if (decidido) return;
      decidido = true;
      resolver(valor);
      cerrar();
    };

    const cabecera = el("div", "dialogo-cabecera");
    const h2 = el("h2", "", titulo);
    h2.id = "dialogo-confirmar-titulo";
    cabecera.append(h2);
    const cuerpo = el("div", "dialogo-cuerpo");
    cuerpo.append(el("p", "", mensaje));
    const pie = el("div", "dialogo-pie");
    const no = el("button", "btn btn-suave", "Cancelar");
    no.type = "button";
    no.addEventListener("click", () => decidir(false));
    const si = el("button", `btn ${peligro ? "btn-peligro" : "btn-primario"}`, textoConfirmar);
    si.type = "button";
    si.addEventListener("click", () => decidir(true));
    pie.append(no, si);
    caja.replaceChildren(cabecera, cuerpo, pie);

    cerrar = abrirDialogo(dialogo);
    dialogo.addEventListener("close", () => decidir(false), { once: true });
    setTimeout(() => no.focus(), 350);
  });
}

// --------------------------------------------------------- selector de foto
const TIPOS_IMAGEN = ["image/jpeg", "image/png", "image/webp"];
const MAXIMO_BYTES = 2 * 1024 * 1024;

/** Zona para elegir o arrastrar una foto, con vista previa. Expone el archivo elegido y si se pidió quitarla. */
export function crearSelectorImagen({ urlActual }) {
  let archivo = null;
  let quitar = false;
  let urlPrevia = null;

  const raiz = el("div", "selector-imagen");
  raiz.append(el("span", "selector-titulo", "Foto del plato"));
  const zona = el("label", "zona-foto");
  const entrada = el("input");
  entrada.type = "file";
  entrada.accept = TIPOS_IMAGEN.join(",");
  entrada.className = "solo-lector";
  const vista = el("div", "zona-vista");
  const texto = el("div", "zona-texto");
  texto.append(el("strong", "", "Arrastra una foto aquí"), el("span", "", "o haz clic para elegirla · JPG, PNG o WebP · máx. 2 MB"));
  zona.append(entrada, vista, texto);
  const error = el("p", "error-campo");
  error.hidden = true;
  const acciones = el("div", "zona-acciones");
  const botonQuitar = el("button", "btn btn-fantasma", "Quitar foto");
  botonQuitar.type = "button";
  acciones.append(botonQuitar);
  raiz.append(zona, error, acciones);

  function pintar() {
    vista.replaceChildren();
    const ruta = urlPrevia ?? (!quitar && urlActual ? window.APP_CONFIG.API_URL + urlActual : null);
    if (ruta) {
      const img = el("img");
      img.src = ruta;
      img.alt = "Vista previa";
      vista.append(img);
      zona.classList.add("con-foto");
    } else {
      const llama = el("img", "zona-llama");
      llama.src = "img/llama.svg";
      llama.alt = "";
      vista.append(llama);
      zona.classList.remove("con-foto");
    }
    botonQuitar.hidden = !ruta;
  }

  function elegir(f) {
    error.hidden = true;
    if (!f) return;
    if (!TIPOS_IMAGEN.includes(f.type)) return mostrarError("Formato no permitido: usa JPG, PNG o WebP.");
    if (f.size > MAXIMO_BYTES) return mostrarError(`La imagen pesa ${(f.size / 1024 / 1024).toFixed(1)} MB; el máximo es 2 MB.`);
    if (urlPrevia) URL.revokeObjectURL(urlPrevia);
    archivo = f;
    quitar = false;
    urlPrevia = URL.createObjectURL(f);
    pintar();
  }

  function mostrarError(t) {
    error.textContent = t;
    error.hidden = false;
    entrada.value = "";
  }

  entrada.addEventListener("change", () => elegir(entrada.files[0]));
  ["dragenter", "dragover"].forEach((n) => zona.addEventListener(n, (e) => { e.preventDefault(); zona.classList.add("arrastrando"); }));
  ["dragleave", "drop"].forEach((n) => zona.addEventListener(n, (e) => { e.preventDefault(); zona.classList.remove("arrastrando"); }));
  zona.addEventListener("drop", (e) => elegir(e.dataTransfer.files[0]));
  botonQuitar.addEventListener("click", () => {
    if (urlPrevia) URL.revokeObjectURL(urlPrevia);
    urlPrevia = null;
    archivo = null;
    quitar = Boolean(urlActual);
    entrada.value = "";
    pintar();
  });

  pintar();
  return { nodo: raiz, archivo: () => archivo, quitar: () => quitar };
}
