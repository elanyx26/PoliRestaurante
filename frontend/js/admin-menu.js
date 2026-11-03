import { api } from "./api.js";
import { avisar, el, mensajeDeError, moneda, urlImagen } from "./ui.js";
import { abrirFormulario, confirmar, crearInterruptor, crearPaginacion, crearSelectorImagen, crearTabla, esqueletos, estadoVacio } from "./admin-ui.js";
import { reiniciarMenu } from "./menu-datos.js";

const TAMANO = 12;

function emblema() {
  const caja = el("div", "prod-sin-foto");
  const img = el("img");
  img.src = "img/llama.svg";
  img.alt = "";
  caja.append(img);
  return caja;
}

export function iniciarMenu(raiz) {
  let subvista = "productos";
  let categorias = [];
  let pagina = 0;
  let versionProductos = 0;
  let temporizadorBusqueda;

  // --------------------------------------------------------------- cabecera
  const cabecera = el("div", "vista-cabecera");
  const titulos = el("div");
  titulos.append(el("h1", "", "Menú"), el("p", "suave", "Productos, fotos y categorías que ven meseros y clientes."));
  const segmentos = el("div", "segmentos");
  const botonProductos = el("button", "segmento", "Productos");
  const botonCategorias = el("button", "segmento", "Categorías");
  botonProductos.type = botonCategorias.type = "button";
  segmentos.append(botonProductos, botonCategorias);
  cabecera.append(titulos, segmentos);

  const zonaProductos = el("div", "subvista");
  const zonaCategorias = el("div", "subvista");
  raiz.append(cabecera, zonaProductos, zonaCategorias);

  function mostrarSubvista(nombre) {
    subvista = nombre;
    botonProductos.setAttribute("aria-pressed", String(nombre === "productos"));
    botonCategorias.setAttribute("aria-pressed", String(nombre === "categorias"));
    zonaProductos.hidden = nombre !== "productos";
    zonaCategorias.hidden = nombre !== "categorias";
    if (nombre === "categorias") cargarCategorias(); else cargarProductos();
  }
  botonProductos.addEventListener("click", () => mostrarSubvista("productos"));
  botonCategorias.addEventListener("click", () => mostrarSubvista("categorias"));

  // ============================================================ PRODUCTOS
  const barra = el("div", "filtros-barra");
  const buscador = el("label", "buscador");
  buscador.append(el("span", "solo-lector", "Buscar producto"));
  buscador.insertAdjacentHTML("beforeend", '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>');
  const campoBusqueda = el("input");
  campoBusqueda.type = "search";
  campoBusqueda.placeholder = "Buscar producto…";
  campoBusqueda.autocomplete = "off";
  buscador.append(campoBusqueda);
  const filtroCategoria = el("select", "control");
  filtroCategoria.setAttribute("aria-label", "Categoría");
  const filtroDisponible = el("select", "control");
  filtroDisponible.setAttribute("aria-label", "Disponibilidad");
  [["", "Todas las disponibilidades"], ["true", "Disponibles"], ["false", "No disponibles"]].forEach(([v, t]) => filtroDisponible.append(new Option(t, v)));
  const nuevoProducto = el("button", "btn btn-primario", "+ Nuevo producto");
  nuevoProducto.type = "button";
  barra.append(buscador, filtroCategoria, filtroDisponible, nuevoProducto);
  const listaProductos = el("div", "vista-lista");
  zonaProductos.append(barra, listaProductos);

  function opcionesCategoria() {
    filtroCategoria.replaceChildren(new Option("Todas las categorías", ""), ...categorias.map((c) => new Option(c.nombre + (c.activa ? "" : " (inactiva)"), String(c.id))));
  }

  campoBusqueda.addEventListener("input", () => {
    clearTimeout(temporizadorBusqueda);
    temporizadorBusqueda = setTimeout(() => { pagina = 0; cargarProductos(); }, 250);
  });
  [filtroCategoria, filtroDisponible].forEach((c) => c.addEventListener("change", () => { pagina = 0; cargarProductos(); }));

  async function cargarCategoriasBase() {
    const r = await api("GET", "/api/admin/categorias?size=100&sort=nombre,asc");
    categorias = r.content;
    const actual = filtroCategoria.value;
    opcionesCategoria();
    filtroCategoria.value = categorias.some((c) => String(c.id) === actual) ? actual : "";
  }

  function tarjetaProducto(p) {
    const tarjeta = el("article", `prod-card${p.disponible ? "" : " agotado"}`);

    const foto = el("button", "prod-foto");
    foto.type = "button";
    foto.setAttribute("aria-label", `Editar ${p.nombre}`);
    const ruta = urlImagen(p.imagenUrl);
    if (ruta) {
      const img = el("img");
      img.src = ruta;
      img.alt = "";
      img.loading = "lazy";
      img.addEventListener("error", () => { img.remove(); foto.prepend(emblema()); });
      foto.append(img);
    } else {
      foto.append(emblema(), el("span", "prod-agregar-foto", "＋ Agregar foto"));
    }
    foto.append(el("span", "prod-precio", moneda.format(p.precio)));
    if (!p.disponible) foto.append(el("span", "prod-etiqueta", "No disponible"));
    foto.addEventListener("click", () => formularioProducto(p));

    const info = el("div", "prod-info");
    info.append(el("h3", "", p.nombre), el("span", "chip-mini", p.categoriaNombre));
    if (p.descripcion) info.append(el("p", "", p.descripcion));

    const pie = el("div", "prod-pie");
    const disponible = el("label", "prod-disponible");
    disponible.append(
      crearInterruptor({
        activo: p.disponible,
        etiqueta: `Disponible: ${p.nombre}`,
        alCambiar: async (nuevo) => {
          await api("PUT", `/api/admin/productos/${p.id}`, cuerpoProducto(p, { disponible: nuevo }));
          p.disponible = nuevo;
          tarjeta.classList.toggle("agotado", !nuevo);
          reiniciarMenu();
          foto.querySelector(".prod-etiqueta")?.remove();
          if (!nuevo) foto.append(el("span", "prod-etiqueta", "No disponible"));
        },
      }),
      el("span", "", "Disponible"),
    );
    const editar = el("button", "btn btn-suave", "Editar");
    editar.type = "button";
    editar.addEventListener("click", () => formularioProducto(p));
    const borrar = el("button", "btn-icono peligro", "🗑");
    borrar.type = "button";
    borrar.title = "Eliminar producto";
    borrar.setAttribute("aria-label", `Eliminar ${p.nombre}`);
    borrar.addEventListener("click", () => eliminarProducto(p));
    pie.append(disponible, editar, borrar);

    tarjeta.append(foto, info, pie);
    return tarjeta;
  }

  const cuerpoProducto = (p, cambios = {}) => ({
    categoriaId: p.categoriaId, nombre: p.nombre, descripcion: p.descripcion ?? null, precio: p.precio, disponible: p.disponible, ...cambios,
  });

  async function cargarProductos() {
    const mi = ++versionProductos;
    if (!listaProductos.children.length) listaProductos.replaceChildren(esqueletos(1, "280px"));
    try {
      if (!categorias.length) await cargarCategoriasBase();
      const q = new URLSearchParams({ page: String(pagina), size: String(TAMANO), sort: "nombre,asc" });
      if (campoBusqueda.value.trim()) q.set("q", campoBusqueda.value.trim());
      if (filtroCategoria.value) q.set("categoriaId", filtroCategoria.value);
      if (filtroDisponible.value) q.set("disponible", filtroDisponible.value);
      const datos = await api("GET", `/api/admin/productos?${q}`);
      if (mi !== versionProductos) return;
      if (!datos.content.length) {
        const crear = el("button", "btn btn-primario", "+ Nuevo producto");
        crear.type = "button";
        crear.addEventListener("click", () => formularioProducto(null));
        listaProductos.replaceChildren(estadoVacio(campoBusqueda.value || filtroCategoria.value || filtroDisponible.value ? "Ningún producto coincide con los filtros." : "Todavía no hay productos.", crear));
        return;
      }
      const rejilla = el("div", "prod-rejilla escalonado");
      datos.content.forEach((p) => rejilla.append(tarjetaProducto(p)));
      listaProductos.replaceChildren(rejilla, crearPaginacion({
        pagina: datos.page, totalPaginas: datos.totalPages, total: datos.totalElements,
        alCambiar: (n) => { pagina = n; cargarProductos(); },
      }));
    } catch (e) {
      if (mi !== versionProductos) return;
      const reintentar = el("button", "btn btn-fantasma", "Reintentar");
      reintentar.type = "button";
      reintentar.addEventListener("click", cargarProductos);
      listaProductos.replaceChildren(estadoVacio(mensajeDeError(e), reintentar));
    }
  }

  /** Crear (p = null) o editar un producto, con su foto en el mismo formulario. */
  function formularioProducto(p) {
    if (!categorias.length) {
      avisar("Primero crea una categoría.", "error");
      mostrarSubvista("categorias");
      return;
    }
    const selector = crearSelectorImagen({ urlActual: p?.imagenUrl ?? null });
    let idActual = p?.id ?? null; // si el producto se crea pero la foto falla, un nuevo intento actualiza en lugar de duplicar
    let imagenActual = p?.imagenUrl ?? null;

    abrirFormulario({
      titulo: p ? "Editar producto" : "Nuevo producto",
      textoGuardar: p ? "Guardar cambios" : "Crear producto",
      campos: [
        { nombre: "nombre", etiqueta: "Nombre", requerido: true, maxLength: 120, ancho: "completo" },
        { nombre: "categoriaId", etiqueta: "Categoría", tipo: "select", requerido: true, opciones: categorias.map((c) => ({ valor: c.id, texto: c.nombre + (c.activa ? "" : " (inactiva)") })) },
        { nombre: "precio", etiqueta: "Precio (COP)", tipo: "number", requerido: true, min: 0, step: 100 },
        { nombre: "descripcion", etiqueta: "Descripción", tipo: "textarea", maxLength: 255, ancho: "completo", ayuda: "Opcional. Aparece en el menú y en la portada." },
        { nombre: "disponible", etiqueta: "Disponible para pedir", tipo: "checkbox", porDefecto: true, ancho: "completo" },
      ],
      valores: p ? { nombre: p.nombre, categoriaId: p.categoriaId, precio: p.precio, descripcion: p.descripcion ?? "", disponible: p.disponible } : {},
      extra: selector.nodo,
      alGuardar: async (v) => {
        const cuerpo = { categoriaId: Number(v.categoriaId), nombre: v.nombre, descripcion: v.descripcion || null, precio: v.precio, disponible: v.disponible };
        const guardado = idActual
          ? await api("PUT", `/api/admin/productos/${idActual}`, cuerpo)
          : await api("POST", "/api/admin/productos", cuerpo);
        idActual = guardado.id;
        imagenActual = guardado.imagenUrl;
        const archivo = selector.archivo();
        if (archivo) {
          const datos = new FormData();
          datos.append("archivo", archivo);
          await api("PUT", `/api/admin/productos/${idActual}/imagen`, datos);
        } else if (selector.quitar() && imagenActual) {
          await api("DELETE", `/api/admin/productos/${idActual}/imagen`);
        }
        reiniciarMenu();
        avisar(p ? "Producto actualizado" : "Producto creado");
        cargarProductos();
      },
    });
  }

  async function eliminarProducto(p) {
    const ok = await confirmar({ titulo: "Eliminar producto", mensaje: `¿Eliminar “${p.nombre}”? Si ya se usó en pedidos no se podrá borrar; en ese caso márcalo como no disponible.`, textoConfirmar: "Eliminar", peligro: true });
    if (!ok) return;
    try {
      await api("DELETE", `/api/admin/productos/${p.id}`);
      reiniciarMenu();
      avisar("Producto eliminado");
      cargarProductos();
    } catch (e) {
      avisar(mensajeDeError(e), "error");
    }
  }
  nuevoProducto.addEventListener("click", () => formularioProducto(null));

  // ========================================================== CATEGORÍAS
  const barraCategorias = el("div", "filtros-barra");
  const nuevaCategoria = el("button", "btn btn-primario", "+ Nueva categoría");
  nuevaCategoria.type = "button";
  barraCategorias.append(nuevaCategoria);
  const listaCategorias = el("div", "vista-lista");
  zonaCategorias.append(barraCategorias, listaCategorias);

  async function cargarCategorias() {
    listaCategorias.replaceChildren(esqueletos(4));
    try {
      await cargarCategoriasBase();
    } catch (e) {
      listaCategorias.replaceChildren(estadoVacio(mensajeDeError(e)));
      return;
    }
    if (!categorias.length) {
      listaCategorias.replaceChildren(estadoVacio("Todavía no hay categorías."));
      return;
    }
    listaCategorias.replaceChildren(crearTabla([
      { titulo: "Nombre", render: (c) => el("strong", "", c.nombre) },
      { titulo: "Descripción", render: (c) => c.descripcion ?? "—" },
      { titulo: "Visible en el menú", render: (c) => crearInterruptor({
        activo: c.activa, etiqueta: `Categoría activa: ${c.nombre}`,
        alCambiar: async (nuevo) => { await api("PUT", `/api/admin/categorias/${c.id}`, { nombre: c.nombre, descripcion: c.descripcion ?? null, activa: nuevo }); c.activa = nuevo; reiniciarMenu(); },
      }) },
      { titulo: "", clase: "derecha acciones", render: (c) => {
        const caja = el("div", "acciones-fila");
        const editar = el("button", "btn btn-suave", "Editar");
        editar.type = "button";
        editar.addEventListener("click", () => formularioCategoria(c));
        const borrar = el("button", "btn-icono peligro", "🗑");
        borrar.type = "button";
        borrar.title = "Eliminar categoría";
        borrar.setAttribute("aria-label", `Eliminar ${c.nombre}`);
        borrar.addEventListener("click", () => eliminarCategoria(c));
        caja.append(editar, borrar);
        return caja;
      } },
    ], categorias));
  }

  function formularioCategoria(c) {
    abrirFormulario({
      titulo: c ? "Editar categoría" : "Nueva categoría",
      textoGuardar: c ? "Guardar cambios" : "Crear categoría",
      campos: [
        { nombre: "nombre", etiqueta: "Nombre", requerido: true, maxLength: 100, ancho: "completo" },
        { nombre: "descripcion", etiqueta: "Descripción", tipo: "textarea", maxLength: 255, ancho: "completo" },
        { nombre: "activa", etiqueta: "Visible en el menú", tipo: "checkbox", porDefecto: true, ancho: "completo" },
      ],
      valores: c ? { nombre: c.nombre, descripcion: c.descripcion ?? "", activa: c.activa } : {},
      alGuardar: async (v) => {
        const cuerpo = { nombre: v.nombre, descripcion: v.descripcion || null, activa: v.activa };
        if (c) await api("PUT", `/api/admin/categorias/${c.id}`, cuerpo); else await api("POST", "/api/admin/categorias", cuerpo);
        reiniciarMenu();
        avisar(c ? "Categoría actualizada" : "Categoría creada");
        await cargarCategorias();
      },
    });
  }

  async function eliminarCategoria(c) {
    const ok = await confirmar({ titulo: "Eliminar categoría", mensaje: `¿Eliminar “${c.nombre}”? Solo se puede si no tiene productos.`, textoConfirmar: "Eliminar", peligro: true });
    if (!ok) return;
    try {
      await api("DELETE", `/api/admin/categorias/${c.id}`);
      reiniciarMenu();
      avisar("Categoría eliminada");
      cargarCategorias();
    } catch (e) {
      avisar(mensajeDeError(e), "error");
    }
  }
  nuevaCategoria.addEventListener("click", () => formularioCategoria(null));

  botonProductos.setAttribute("aria-pressed", "true");
  botonCategorias.setAttribute("aria-pressed", "false");
  zonaCategorias.hidden = true;
  return { mostrar: () => (subvista === "categorias" ? cargarCategorias() : cargarProductos()) };
}
