import { api } from "./api.js";
import { avisar, el, mensajeDeError } from "./ui.js";
import { abrirFormulario, confirmar, crearInterruptor, crearPaginacion, crearTabla, esqueletos, estadoVacio, fechaLarga } from "./admin-ui.js";

const TAMANO = 12;
const NOMBRE_ROL = { ADMINISTRADOR: "Administrador", MESERO: "Mesero", COCINA: "Cocina" };
const OPCIONES_ROL = Object.entries(NOMBRE_ROL).map(([valor, texto]) => ({ valor, texto }));

export function iniciarUsuarios(raiz, { usuarioActualId }) {
  let pagina = 0;
  let version = 0;
  let temporizadorBusqueda;

  const cabecera = el("div", "vista-cabecera");
  const titulos = el("div");
  titulos.append(el("h1", "", "Usuarios"), el("p", "suave", "Cuentas del personal. Desactivar una cuenta le quita el acceso al instante."));
  const nuevo = el("button", "btn btn-primario", "+ Nuevo usuario");
  nuevo.type = "button";
  cabecera.append(titulos, nuevo);

  const barra = el("div", "filtros-barra");
  const buscador = el("label", "buscador");
  buscador.append(el("span", "solo-lector", "Buscar usuario"));
  buscador.insertAdjacentHTML("beforeend", '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>');
  const busqueda = el("input");
  busqueda.type = "search";
  busqueda.placeholder = "Nombre o correo…";
  busqueda.autocomplete = "off";
  buscador.append(busqueda);
  const filtroRol = el("select", "control");
  filtroRol.setAttribute("aria-label", "Rol");
  filtroRol.append(new Option("Todos los roles", ""), ...OPCIONES_ROL.map((o) => new Option(o.texto, o.valor)));
  const filtroEstado = el("select", "control");
  filtroEstado.setAttribute("aria-label", "Estado");
  [["", "Activos e inactivos"], ["true", "Solo activos"], ["false", "Solo inactivos"]].forEach(([v, t]) => filtroEstado.append(new Option(t, v)));
  barra.append(buscador, filtroRol, filtroEstado);

  const lista = el("div", "vista-lista");
  raiz.append(cabecera, barra, lista);

  busqueda.addEventListener("input", () => {
    clearTimeout(temporizadorBusqueda);
    temporizadorBusqueda = setTimeout(() => { pagina = 0; cargar(); }, 250);
  });
  [filtroRol, filtroEstado].forEach((c) => c.addEventListener("change", () => { pagina = 0; cargar(); }));

  async function cargar() {
    const mi = ++version;
    if (!lista.children.length) lista.replaceChildren(esqueletos(6));
    const q = new URLSearchParams({ page: String(pagina), size: String(TAMANO), sort: "nombre,asc" });
    if (busqueda.value.trim()) q.set("q", busqueda.value.trim());
    if (filtroRol.value) q.set("rol", filtroRol.value);
    if (filtroEstado.value) q.set("activo", filtroEstado.value);
    try {
      const datos = await api("GET", `/api/admin/usuarios?${q}`);
      if (mi !== version) return;
      if (!datos.content.length) {
        lista.replaceChildren(estadoVacio("No hay usuarios con estos filtros."));
        return;
      }
      lista.replaceChildren(
        crearTabla([
          { titulo: "Nombre", render: (u) => {
            const caja = el("div", "usuario-celda");
            caja.append(el("span", "avatar", u.nombre.charAt(0).toUpperCase()), el("strong", "", u.nombre + (u.id === usuarioActualId ? " (tú)" : "")));
            return caja;
          } },
          { titulo: "Correo", render: (u) => u.email },
          { titulo: "Rol", render: (u) => el("span", `rol-pill ${u.rol}`, NOMBRE_ROL[u.rol] ?? u.rol) },
          { titulo: "Creado", render: (u) => fechaLarga(u.createdAt) },
          { titulo: "Activo", render: (u) => crearInterruptor({
            activo: u.activo, etiqueta: `Cuenta activa: ${u.nombre}`,
            alCambiar: async (valor) => {
              if (!valor && u.id === usuarioActualId) {
                const ok = await confirmar({ titulo: "Desactivar tu propia cuenta", mensaje: "Perderás el acceso en este mismo momento. ¿Continuar?", textoConfirmar: "Desactivar", peligro: true });
                if (!ok) throw Object.assign(new Error("Cancelado"), { cancelado: true });
              }
              await api("PATCH", `/api/admin/usuarios/${u.id}/estado`, { activo: valor });
              u.activo = valor;
              avisar(valor ? `${u.nombre} activado` : `${u.nombre} desactivado`);
            },
          }) },
          { titulo: "", clase: "derecha acciones", render: (u) => {
            const caja = el("div", "acciones-fila");
            const editar = el("button", "btn btn-suave", "Editar");
            editar.type = "button";
            editar.addEventListener("click", () => formulario(u));
            const clave = el("button", "btn-icono", "🔑");
            clave.type = "button";
            clave.title = "Cambiar contraseña";
            clave.setAttribute("aria-label", `Cambiar contraseña de ${u.nombre}`);
            clave.addEventListener("click", () => cambiarContrasena(u));
            caja.append(editar, clave);
            return caja;
          } },
        ], datos.content),
        crearPaginacion({ pagina: datos.page, totalPaginas: datos.totalPages, total: datos.totalElements, alCambiar: (n) => { pagina = n; cargar(); } }),
      );
    } catch (e) {
      if (mi !== version) return;
      const reintentar = el("button", "btn btn-fantasma", "Reintentar");
      reintentar.type = "button";
      reintentar.addEventListener("click", cargar);
      lista.replaceChildren(estadoVacio(mensajeDeError(e), reintentar));
    }
  }

  function formulario(u) {
    const campos = [
      { nombre: "nombre", etiqueta: "Nombre completo", requerido: true, maxLength: 120, ancho: "completo" },
      { nombre: "email", etiqueta: "Correo", tipo: "email", requerido: true, maxLength: 150, ancho: "completo" },
      { nombre: "rol", etiqueta: "Rol", tipo: "select", requerido: true, opciones: OPCIONES_ROL },
    ];
    if (!u) campos.push({ nombre: "password", etiqueta: "Contraseña inicial", tipo: "password", requerido: true, minLength: 8, maxLength: 72, ayuda: "Mínimo 8 caracteres. El usuario podrá pedir que se la cambien." });
    abrirFormulario({
      titulo: u ? "Editar usuario" : "Nuevo usuario",
      textoGuardar: u ? "Guardar cambios" : "Crear usuario",
      campos,
      valores: u ? { nombre: u.nombre, email: u.email, rol: u.rol } : { rol: "MESERO" },
      alGuardar: async (v) => {
        if (u) await api("PUT", `/api/admin/usuarios/${u.id}`, { nombre: v.nombre, email: v.email, rol: v.rol });
        else await api("POST", "/api/usuarios", { nombre: v.nombre, email: v.email, password: v.password, rol: v.rol });
        avisar(u ? "Usuario actualizado" : "Usuario creado");
        cargar();
      },
    });
  }

  function cambiarContrasena(u) {
    abrirFormulario({
      titulo: `Nueva contraseña para ${u.nombre}`,
      subtitulo: "La contraseña anterior dejará de funcionar. Compártela con la persona por un canal seguro.",
      textoGuardar: "Cambiar contraseña",
      campos: [{ nombre: "nuevaContrasena", etiqueta: "Nueva contraseña", tipo: "password", requerido: true, minLength: 8, maxLength: 72, ancho: "completo" }],
      alGuardar: async (v) => {
        await api("PATCH", `/api/admin/usuarios/${u.id}/contrasena`, { nuevaContrasena: v.nuevaContrasena });
        avisar("Contraseña actualizada");
      },
    });
  }

  nuevo.addEventListener("click", () => formulario(null));
  return { mostrar: cargar };
}
