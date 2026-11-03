import { ApiError, PANTALLAS, api, guardarSesion, sesion } from "./api.js";
import { crearBrasas } from "./ui.js";

const formulario = document.getElementById("form-login");
const tarjeta = document.querySelector(".login-tarjeta");
const aviso = document.getElementById("aviso");
const boton = document.getElementById("btn-entrar");
const campoClave = document.getElementById("password");
const botonOjo = document.getElementById("ver-clave");

crearBrasas(14);

function mostrar(texto, tipo = "error") {
  aviso.textContent = texto;
  aviso.className = `aviso ${tipo}`;
  aviso.hidden = !texto;
  if (texto && tipo === "error") {
    // Reinicia la animación de sacudida aunque el error se repita.
    tarjeta.classList.remove("error");
    void tarjeta.offsetWidth;
    tarjeta.classList.add("error");
  }
}

const existente = sesion();
if (existente && PANTALLAS[existente.usuario.rol]) {
  location.replace(PANTALLAS[existente.usuario.rol]);
}

const mensaje = new URLSearchParams(location.search).get("mensaje");
if (mensaje) mostrar(mensaje, "info");

botonOjo.addEventListener("click", () => {
  const visible = campoClave.type === "password";
  campoClave.type = visible ? "text" : "password";
  botonOjo.setAttribute("aria-pressed", String(visible));
  botonOjo.setAttribute("aria-label", visible ? "Ocultar contraseña" : "Mostrar contraseña");
});

formulario.addEventListener("submit", async (evento) => {
  evento.preventDefault();
  mostrar("");
  boton.classList.add("cargando");
  try {
    const login = await api("POST", "/api/auth/login", {
      email: formulario.email.value.trim(),
      password: formulario.password.value,
    });
    const destino = PANTALLAS[login.usuario.rol];
    if (!destino) {
      mostrar(`La pantalla para el rol ${login.usuario.rol} todavía no está disponible.`, "info");
      return;
    }
    guardarSesion(login);
    location.href = destino;
  } catch (e) {
    if (e instanceof ApiError && e.estado === 429) {
      mostrar("Demasiados intentos fallidos. Espere unos minutos.");
    } else if (e instanceof ApiError && e.estado === 400) {
      mostrar("Escriba un correo válido y su contraseña.");
    } else if (e instanceof ApiError && e.estado === 401) {
      mostrar("Correo o contraseña incorrectos.");
    } else {
      mostrar(e.message);
    }
  } finally {
    boton.classList.remove("cargando");
  }
});
