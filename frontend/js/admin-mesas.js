import { api } from "./api.js";
import { avisar, el, mensajeDeError } from "./ui.js";
import { abrirFormulario, confirmar, esqueletos, estadoVacio } from "./admin-ui.js";

export function iniciarMesas(raiz) {
  let version = 0;
  let temporizador;

  const cabecera = el("div", "vista-cabecera");
  const titulos = el("div");
  titulos.append(el("h1", "", "Mesas"), el("p", "suave", "El estado lo controlan los pedidos: se ocupa al abrir uno y se libera al entregar o cancelar."));
  const nueva = el("button", "btn btn-primario", "+ Nueva mesa");
  nueva.type = "button";
  cabecera.append(titulos, nueva);

  const resumen = el("div", "mini-kpis");
  const lista = el("div", "vista-lista");
  raiz.append(cabecera, resumen, lista);

  function tarjeta(m) {
    const t = el("article", `mesa-card ${m.estado}`);
    const cabeza = el("div", "mesa-cabeza");
    cabeza.append(el("span", "mesa-etiqueta", "Mesa"), el("strong", "mesa-numero", String(m.numero)));
    t.append(cabeza);
    t.append(el("span", `estado-mesa ${m.estado}`, m.estado === "LIBRE" ? "Libre" : "Ocupada"));
    t.append(el("span", "suave", `👥 ${m.capacidad} personas`));

    const acciones = el("div", "acciones-fila");
    const editar = el("button", "btn btn-suave", "Editar");
    editar.type = "button";
    editar.addEventListener("click", () => formulario(m));
    const borrar = el("button", "btn-icono peligro", "🗑");
    borrar.type = "button";
    borrar.title = "Eliminar mesa";
    borrar.setAttribute("aria-label", `Eliminar mesa ${m.numero}`);
    borrar.addEventListener("click", () => eliminar(m));
    acciones.append(editar, borrar);
    t.append(acciones);
    return t;
  }

  async function cargar() {
    const mi = ++version;
    if (!lista.children.length) lista.replaceChildren(esqueletos(1, "170px"));
    try {
      const datos = await api("GET", "/api/admin/mesas?size=100&sort=numero,asc");
      if (mi !== version) return;
      const mesas = datos.content;
      const libres = mesas.filter((m) => m.estado === "LIBRE").length;
      resumen.replaceChildren(...[["Total", mesas.length], ["Libres", libres], ["Ocupadas", mesas.length - libres]].map(([n, v]) => {
        const c = el("div", "mini-kpi");
        c.append(el("span", "", n), el("strong", "", String(v)));
        return c;
      }));
      if (!mesas.length) {
        lista.replaceChildren(estadoVacio("Todavía no hay mesas."));
        return;
      }
      const rejilla = el("div", "mesas-rejilla escalonado");
      mesas.forEach((m) => rejilla.append(tarjeta(m)));
      lista.replaceChildren(rejilla);
    } catch (e) {
      if (mi !== version) return;
      const reintentar = el("button", "btn btn-fantasma", "Reintentar");
      reintentar.type = "button";
      reintentar.addEventListener("click", cargar);
      lista.replaceChildren(estadoVacio(mensajeDeError(e), reintentar));
    }
  }

  function formulario(m) {
    abrirFormulario({
      titulo: m ? `Editar mesa ${m.numero}` : "Nueva mesa",
      textoGuardar: m ? "Guardar cambios" : "Crear mesa",
      campos: [
        { nombre: "numero", etiqueta: "Número de mesa", tipo: "number", requerido: true, min: 1, step: 1 },
        { nombre: "capacidad", etiqueta: "Capacidad (personas)", tipo: "number", requerido: true, min: 1, step: 1 },
      ],
      valores: m ? { numero: m.numero, capacidad: m.capacidad } : { capacidad: 4 },
      alGuardar: async (v) => {
        const cuerpo = { numero: v.numero, capacidad: v.capacidad };
        if (m) await api("PUT", `/api/admin/mesas/${m.id}`, cuerpo); else await api("POST", "/api/admin/mesas", cuerpo);
        avisar(m ? "Mesa actualizada" : "Mesa creada");
        cargar();
      },
    });
  }

  async function eliminar(m) {
    const ok = await confirmar({ titulo: "Eliminar mesa", mensaje: `¿Eliminar la mesa ${m.numero}? Solo se puede si no está ocupada ni tiene pedidos registrados.`, textoConfirmar: "Eliminar", peligro: true });
    if (!ok) return;
    try {
      await api("DELETE", `/api/admin/mesas/${m.id}`);
      avisar("Mesa eliminada");
      cargar();
    } catch (e) {
      avisar(mensajeDeError(e), "error");
    }
  }

  nueva.addEventListener("click", () => formulario(null));
  return {
    mostrar: cargar,
    actualizar() { clearTimeout(temporizador); temporizador = setTimeout(cargar, 300); },
  };
}
