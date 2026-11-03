import { api } from "./api.js";
import { avisar, el, mensajeDeError, moneda } from "./ui.js";
import { esqueletos, estadoVacio, fechaLarga } from "./admin-ui.js";

export function iniciarConfiguracion(raiz) {
  const cabecera = el("div", "vista-cabecera");
  const titulos = el("div");
  titulos.append(el("h1", "", "Configuración"), el("p", "suave", "Reglas de pago para los pedidos a domicilio."));
  cabecera.append(titulos);

  const contenido = el("div", "vista-lista");
  raiz.append(cabecera, contenido);

  function pintar(c) {
    const tarjeta = el("section", "panel-tarjeta config-tarjeta");
    const encabezado = el("div", "panel-encabezado");
    encabezado.append(el("h2", "", "Límite de pago contraentrega"));
    tarjeta.append(encabezado);
    tarjeta.append(el("p", "suave", "Si el total de un domicilio supera este valor, el mesero debe elegir PAGO_VIRTUAL. El sistema no procesa el pago: solo valida el método elegido."));

    const actual = el("div", "config-actual");
    actual.append(el("span", "suave", "Valor actual"), el("strong", "config-valor", moneda.format(c.limiteContraentrega)));
    tarjeta.append(actual);

    const formulario = el("form", "config-formulario");
    const campo = el("label", "campo");
    campo.append(el("span", "", "Nuevo límite (COP)"));
    const entrada = el("input", "control");
    entrada.type = "number";
    entrada.min = "1";
    entrada.step = "1";
    entrada.required = true;
    entrada.value = String(Math.round(Number(c.limiteContraentrega)));
    campo.append(entrada);
    const error = el("p", "aviso error", "");
    error.hidden = true;
    error.setAttribute("role", "alert");
    const guardar = el("button", "btn btn-primario", "Guardar límite");
    guardar.type = "submit";
    formulario.append(campo, error, guardar);
    tarjeta.append(formulario);

    if (c.actualizadoPor) tarjeta.append(el("small", "suave", `Última modificación: ${fechaLarga(c.actualizadoEn)} · ${c.actualizadoPor}`));
    else tarjeta.append(el("small", "suave", `Configuración inicial · ${fechaLarga(c.actualizadoEn)}`));

    formulario.addEventListener("submit", async (e) => {
      e.preventDefault();
      error.hidden = true;
      guardar.classList.add("cargando");
      try {
        const nuevo = await api("PUT", "/api/admin/configuracion/domicilios", { limiteContraentrega: Number(entrada.value) });
        avisar("Límite actualizado");
        contenido.replaceChildren(pintar(nuevo));
      } catch (err) {
        error.textContent = mensajeDeError(err);
        error.hidden = false;
      } finally {
        guardar.classList.remove("cargando");
      }
    });
    return tarjeta;
  }

  async function cargar() {
    contenido.replaceChildren(esqueletos(1, "320px"));
    try {
      contenido.replaceChildren(pintar(await api("GET", "/api/admin/configuracion/domicilios")));
    } catch (e) {
      const reintentar = el("button", "btn btn-fantasma", "Reintentar");
      reintentar.type = "button";
      reintentar.addEventListener("click", cargar);
      contenido.replaceChildren(estadoVacio(mensajeDeError(e), reintentar));
    }
  }

  return { mostrar: cargar };
}
