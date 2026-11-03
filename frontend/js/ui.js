import { api } from "./api.js";

/** Crea un elemento sin innerHTML: lo que venga de la API nunca se interpreta como HTML. */
export function el(etiqueta, clase, texto) {
  const nodo = document.createElement(etiqueta);
  if (clase) nodo.className = clase;
  if (texto !== undefined) nodo.textContent = texto;
  return nodo;
}

export const reducirMovimiento = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

export const moneda = new Intl.NumberFormat("es-CO", {
  style: "currency",
  currency: "COP",
  maximumFractionDigits: 0,
});

// ----------------------------------------------------------------- avisos
let temporizador;
export function avisar(mensaje, tipo = "info") {
  let toast = document.getElementById("toast");
  if (!toast) {
    toast = el("div", "toast");
    toast.id = "toast";
    toast.setAttribute("role", "status");
    toast.setAttribute("aria-live", "polite");
    document.body.append(toast);
  }
  toast.textContent = mensaje;
  toast.className = `toast ${tipo}`;
  // Fuerza un reflujo para que la transición de entrada se reproduzca siempre.
  void toast.offsetWidth;
  toast.classList.add("visible");
  clearTimeout(temporizador);
  temporizador = setTimeout(() => toast.classList.remove("visible"), 4200);
}

// ----------------------------------------------- aparición al hacer scroll
let observador;
export function revelar(contenedor = document) {
  const nodos = contenedor.querySelectorAll(".revelar:not(.visible)");
  if (reducirMovimiento || !("IntersectionObserver" in window)) {
    nodos.forEach((n) => n.classList.add("visible"));
    return;
  }
  observador = observador || new IntersectionObserver((entradas) => {
    for (const entrada of entradas) {
      if (entrada.isIntersecting) {
        entrada.target.classList.add("visible");
        observador.unobserve(entrada.target);
      }
    }
  }, { threshold: 0.12 });
  nodos.forEach((n) => observador.observe(n));
}

// ------------------------------------------------------------ brasas de fondo
export function crearBrasas(cantidad = 22) {
  if (reducirMovimiento) return;
  const contenedor = el("div", "brasas");
  contenedor.setAttribute("aria-hidden", "true");
  for (let i = 0; i < cantidad; i++) {
    const brasa = el("span");
    brasa.style.setProperty("--x", `${Math.random() * 100}%`);
    brasa.style.setProperty("--tam", `${3 + Math.random() * 6}px`);
    brasa.style.setProperty("--dur", `${9 + Math.random() * 12}s`);
    brasa.style.setProperty("--ret", `${-Math.random() * 20}s`);
    brasa.style.setProperty("--deriva", `${(Math.random() - 0.5) * 160}px`);
    contenedor.append(brasa);
  }
  document.body.prepend(contenedor);
}

// ------------------------------------------------------------------ imágenes
/** La API devuelve rutas relativas (/api/public/imagenes/...); se completan con la dirección del backend. */
export function urlImagen(ruta) {
  return ruta ? window.APP_CONFIG.API_URL + ruta : null;
}

export function obtenerMenuPublico() {
  return api("GET", "/api/public/menu");
}

/** Primer mensaje de error legible que devolvió el backend (los errores de validación traen un mapa campo → mensaje). */
export function mensajeDeError(e) {
  if (e && e.errores) return Object.values(e.errores)[0];
  return e?.message ?? "Error inesperado";
}

/** "hace 5 min", "hace 2 h 10 min"… a partir de una fecha ISO. */
export function tiempoRelativo(fechaIso) {
  const minutos = Math.max(0, Math.floor((Date.now() - new Date(fechaIso).getTime()) / 60000));
  if (minutos < 1) return "hace menos de 1 min";
  if (minutos < 60) return `hace ${minutos} min`;
  if (minutos < 1440) return `hace ${Math.floor(minutos / 60)} h ${minutos % 60} min`;
  return new Date(fechaIso).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" });
}

/** Selector de cantidad [−] n [+]. Devuelve { nodo, fijar(valor) } para actualizarlo sin volver a crearlo. */
export function crearPaso(valor, { alRestar, alSumar, minimo = 1 }) {
  const nodo = el("div", "paso");
  nodo.setAttribute("role", "group");
  nodo.setAttribute("aria-label", "Cantidad");
  const menos = el("button", "", "−");
  const mas = el("button", "", "+");
  menos.type = mas.type = "button";
  menos.setAttribute("aria-label", "Quitar uno");
  mas.setAttribute("aria-label", "Agregar uno");
  const salida = el("output", "", String(valor));
  menos.addEventListener("click", alRestar);
  mas.addEventListener("click", alSumar);
  nodo.append(menos, salida, mas);
  return {
    nodo,
    fijar(nuevo) {
      salida.textContent = String(nuevo);
      menos.disabled = nuevo <= minimo && minimo > 0;
    },
    bloquear(si) { menos.disabled = mas.disabled = si; },
  };
}

/** Abre un <dialog> con animación de entrada y devuelve la función que lo cierra con animación de salida. */
export function abrirDialogo(dialogo) {
  let cerrando = false;
  dialogo.showModal();
  // Un pequeño retardo (en lugar de requestAnimationFrame) mantiene la animación incluso con la pestaña en segundo plano.
  setTimeout(() => dialogo.classList.add("abierto"), 20);

  function cerrar() {
    if (cerrando || !dialogo.open) return;
    cerrando = true;
    dialogo.classList.remove("abierto");
    setTimeout(() => dialogo.close(), reducirMovimiento ? 0 : 320);
  }
  dialogo.onclick = (e) => { if (e.target === dialogo) cerrar(); };
  dialogo.oncancel = (e) => { e.preventDefault(); cerrar(); };
  dialogo.onkeydown = (e) => { if (e.key === "Escape") { e.preventDefault(); cerrar(); } };
  return cerrar;
}
