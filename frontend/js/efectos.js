import { el, reducirMovimiento } from "./ui.js";

const PALABRAS_BANDA = ["Ingredientes frescos", "Hecho al momento", "En el salón", "Para llevar", "A domicilio"];

/** Una sola escucha de scroll (con requestAnimationFrame) mueve la barra, el parallax del hero y la franja. */
export function iniciarEfectosDeScroll() {
  const progreso = document.getElementById("progreso");
  const hero = document.querySelector(".hero");
  const pista = document.getElementById("banda-pista");
  const enlaces = new Map([...document.querySelectorAll('.navegacion a[href^="#"]')].map((a) => [a.getAttribute("href").slice(1), a]));
  const secciones = [...enlaces.keys()].map((id) => document.getElementById(id)).filter(Boolean);
  let pendiente = false;
  let anchoGrupo = 0;
  let seccionActiva = "";

  if (pista) {
    // Dos copias idénticas: al desplazar exactamente el ancho de una, el bucle es imperceptible.
    for (let copia = 0; copia < 2; copia++) {
      const grupo = el("div", "banda-grupo");
      PALABRAS_BANDA.forEach((texto) => grupo.append(el("span", "", texto), el("i", "", "✦")));
      pista.append(grupo);
    }
  }
  const medir = () => { if (pista) anchoGrupo = pista.scrollWidth / 2; };

  function actualizar() {
    pendiente = false;
    const y = window.scrollY;
    const recorrido = document.documentElement.scrollHeight - window.innerHeight;
    progreso.style.transform = `scaleX(${recorrido > 0 ? Math.min(1, y / recorrido) : 0})`;

    // La sección activa es la última cuyo borde superior ya pasó la línea de lectura (45 % de la pantalla).
    const linea = window.innerHeight * 0.45;
    let activa = secciones[0]?.id ?? "";
    for (const s of secciones) if (s.getBoundingClientRect().top <= linea) activa = s.id;
    if (activa !== seccionActiva) {
      seccionActiva = activa;
      enlaces.forEach((enlace, id) => enlace.classList.toggle("activo", id === activa));
    }
    if (reducirMovimiento) return;

    if (hero) {
      const p = Math.min(1, Math.max(0, y / (hero.offsetHeight * 0.85)));
      hero.style.setProperty("--p", p.toFixed(3));
    }
    if (pista && anchoGrupo) {
      pista.style.transform = `translate3d(${-((y * 0.5) % anchoGrupo)}px, 0, 0)`;
    }
  }

  window.addEventListener("scroll", () => {
    if (!pendiente) {
      pendiente = true;
      requestAnimationFrame(actualizar);
    }
  }, { passive: true });
  window.addEventListener("resize", () => { medir(); actualizar(); });
  // Las medidas dependen de la tipografía cargada.
  (document.fonts?.ready ?? Promise.resolve()).then(() => { medir(); actualizar(); });
  medir();
  actualizar();
}

/** Parte el título en palabras para animarlas una a una (sin innerHTML). */
export function dividirPalabras(titulo) {
  const texto = titulo.textContent.trim();
  titulo.setAttribute("aria-label", texto);
  titulo.replaceChildren();
  texto.split(/\s+/).forEach((palabra, i) => {
    const caja = el("span", "palabra");
    caja.setAttribute("aria-hidden", "true");
    const interior = el("span", "palabra-int", palabra);
    interior.style.setProperty("--i", i);
    caja.append(interior);
    titulo.append(caja, document.createTextNode(" "));
  });
}

const PUNTERO_FINO = window.matchMedia("(hover: hover) and (pointer: fine)");

/** Inclinación 3D con brillo que sigue al cursor; solo con mouse y sin "reducir movimiento". */
export function inclinar(tarjeta) {
  if (reducirMovimiento || !PUNTERO_FINO.matches) return;
  tarjeta.addEventListener("pointermove", (e) => {
    const r = tarjeta.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width;
    const y = (e.clientY - r.top) / r.height;
    tarjeta.classList.add("inclinando");
    tarjeta.style.setProperty("--ry", `${((x - 0.5) * 12).toFixed(2)}deg`);
    tarjeta.style.setProperty("--rx", `${((0.5 - y) * 12).toFixed(2)}deg`);
    tarjeta.style.setProperty("--gx", `${(x * 100).toFixed(1)}%`);
    tarjeta.style.setProperty("--gy", `${(y * 100).toFixed(1)}%`);
  });
  tarjeta.addEventListener("pointerleave", () => {
    tarjeta.classList.remove("inclinando");
    ["--rx", "--ry", "--gx", "--gy"].forEach((v) => tarjeta.style.removeProperty(v));
  });
}
