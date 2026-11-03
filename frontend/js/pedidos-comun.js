import { el } from "./ui.js";

export const NOMBRE_ESTADO = {
  PENDIENTE: "Pendiente",
  EN_PREPARACION: "En preparación",
  LISTO: "Listo",
  ENTREGADO: "Entregado",
  CANCELADO: "Cancelado",
};
export const NOMBRE_PAGO = { EFECTIVO_CONTRAENTREGA: "Contraentrega (efectivo)", PAGO_VIRTUAL: "Pago virtual" };
export const NOMBRE_TIPO = { SALON: "Salón", PARA_LLEVAR: "Para llevar", DOMICILIO: "Domicilio" };

export const hora = (iso) => new Date(iso).toLocaleString("es-CO", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });

export function insignia(estado) {
  return el("span", `estado ${estado}`, NOMBRE_ESTADO[estado] ?? estado);
}

/** "Mesa 4", "Para llevar · Marta" o "Domicilio · Luis" (sirve para el resumen y para el detalle del pedido). */
export function destino(p) {
  const mesa = p.mesaNumero ?? p.mesa?.numero;
  if (p.tipo === "SALON") return `Mesa ${mesa ?? "?"}`;
  return `${NOMBRE_TIPO[p.tipo] ?? p.tipo} · ${p.nombreCliente}`;
}
