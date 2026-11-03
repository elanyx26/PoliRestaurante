const { API_URL } = window.APP_CONFIG;
const CLAVE_SESION = "poli.sesion";

/** Pantalla de inicio de cada rol (solo existen las que ya se construyeron). */
export const PANTALLAS = {
  ADMINISTRADOR: "admin.html",
  COCINA: "cocina.html",
  MESERO: "mesero.html",
};

export class ApiError extends Error {
  constructor(estado, mensaje, errores) {
    super(mensaje);
    this.estado = estado;
    this.errores = errores;
  }
}

// La sesión vive en sessionStorage: se borra al cerrar la pestaña.
export function guardarSesion(login) {
  sessionStorage.setItem(CLAVE_SESION, JSON.stringify({ token: login.accessToken, usuario: login.usuario }));
}

export function sesion() {
  try {
    return JSON.parse(sessionStorage.getItem(CLAVE_SESION));
  } catch {
    return null;
  }
}

export function cerrarSesion(mensaje) {
  sessionStorage.removeItem(CLAVE_SESION);
  location.href = "login.html" + (mensaje ? `?mensaje=${encodeURIComponent(mensaje)}` : "");
}

/** Devuelve la sesión si el usuario tiene el rol indicado; si no, lo manda al login. */
export function exigirSesion(rol) {
  const s = sesion();
  if (!s || s.usuario.rol !== rol) {
    cerrarSesion();
    return null;
  }
  return s;
}

export async function api(metodo, ruta, cuerpo) {
  const s = sesion();
  const headers = {};
  const esArchivo = cuerpo instanceof FormData;
  // Con FormData el navegador fija el Content-Type (multipart) con su boundary: no se debe forzar.
  if (cuerpo !== undefined && !esArchivo) headers["Content-Type"] = "application/json";
  if (s) headers.Authorization = `Bearer ${s.token}`;

  let respuesta;
  try {
    respuesta = await fetch(API_URL + ruta, {
      method: metodo,
      headers,
      body: cuerpo === undefined ? undefined : esArchivo ? cuerpo : JSON.stringify(cuerpo),
    });
  } catch {
    throw new ApiError(0, "No se pudo conectar con el servidor");
  }

  if (respuesta.status === 204) return null;
  const texto = await respuesta.text();
  let datos = null;
  try {
    datos = texto ? JSON.parse(texto) : null;
  } catch {
    // respuesta sin JSON
  }

  if (!respuesta.ok) {
    if (respuesta.status === 401 && s) {
      cerrarSesion("Su sesión venció. Inicie sesión de nuevo.");
    }
    throw new ApiError(respuesta.status, datos?.detail || "Error inesperado", datos?.errores);
  }
  return datos;
}

export function urlWebSocket() {
  return API_URL.replace(/^http/, "ws") + "/ws";
}
