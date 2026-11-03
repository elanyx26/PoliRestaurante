import { api } from "./api.js";

let promesa;

/** Categorías y productos disponibles (menú del personal). Se carga una vez y se comparte entre pantallas. */
export function obtenerMenu() {
  if (!promesa) {
    promesa = Promise.all([api("GET", "/api/menu/categorias"), api("GET", "/api/menu/productos")])
      .then(([categorias, productos]) => ({ categorias, productos }));
    promesa.catch(() => { promesa = null; });
  }
  return promesa;
}

export function reiniciarMenu() {
  promesa = null;
}
