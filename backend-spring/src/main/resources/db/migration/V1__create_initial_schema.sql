-- Esquema inicial de PoliRestaurante (PostgreSQL).

CREATE TABLE usuarios (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    rol            VARCHAR(20)  NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMINISTRADOR', 'MESERO', 'COCINA'))
);
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (lower(email));
CREATE INDEX ix_usuarios_rol ON usuarios (rol);

CREATE TABLE mesas (
    id          BIGSERIAL PRIMARY KEY,
    numero      INTEGER     NOT NULL,
    capacidad   INTEGER     NOT NULL DEFAULT 4,
    estado      VARCHAR(10) NOT NULL DEFAULT 'LIBRE',
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_mesas_numero UNIQUE (numero),
    CONSTRAINT ck_mesas_numero CHECK (numero > 0),
    CONSTRAINT ck_mesas_capacidad CHECK (capacidad > 0),
    CONSTRAINT ck_mesas_estado CHECK (estado IN ('LIBRE', 'OCUPADA'))
);
CREATE INDEX ix_mesas_estado ON mesas (estado);

CREATE TABLE categorias (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    descripcion  VARCHAR(255),
    activa       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_categorias_nombre ON categorias (lower(nombre));

CREATE TABLE productos (
    id            BIGSERIAL PRIMARY KEY,
    categoria_id  BIGINT         NOT NULL REFERENCES categorias (id),
    nombre        VARCHAR(120)   NOT NULL,
    descripcion   VARCHAR(255),
    precio        NUMERIC(12, 2) NOT NULL,
    disponible    BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_productos_precio CHECK (precio >= 0)
);
CREATE UNIQUE INDEX ux_productos_categoria_nombre ON productos (categoria_id, lower(nombre));

CREATE TABLE configuracion_domicilios (
    id                    BIGSERIAL PRIMARY KEY,
    limite_contraentrega  NUMERIC(12, 2) NOT NULL,
    activa                BOOLEAN        NOT NULL DEFAULT TRUE,
    created_by            BIGINT REFERENCES usuarios (id),
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_config_limite CHECK (limite_contraentrega > 0)
);
-- Solo puede haber una configuración activa; las anteriores quedan como historial.
CREATE UNIQUE INDEX ux_config_domicilios_activa ON configuracion_domicilios (activa) WHERE activa;

CREATE TABLE pedidos (
    id                BIGSERIAL PRIMARY KEY,
    tipo              VARCHAR(15)    NOT NULL,
    estado            VARCHAR(15)    NOT NULL,
    mesero_id         BIGINT         NOT NULL REFERENCES usuarios (id),
    mesa_id           BIGINT REFERENCES mesas (id),
    nombre_cliente    VARCHAR(120),
    direccion_entrega VARCHAR(255),
    telefono_entrega  VARCHAR(30),
    costo_envio       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    metodo_pago       VARCHAR(30),
    total             NUMERIC(12, 2) NOT NULL DEFAULT 0,
    entregado_en      TIMESTAMPTZ,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_pedidos_tipo CHECK (tipo IN ('SALON', 'PARA_LLEVAR', 'DOMICILIO')),
    CONSTRAINT ck_pedidos_estado CHECK (estado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO', 'ENTREGADO', 'CANCELADO')),
    CONSTRAINT ck_pedidos_metodo_pago CHECK (metodo_pago IS NULL OR metodo_pago IN ('EFECTIVO_CONTRAENTREGA', 'PAGO_VIRTUAL')),
    CONSTRAINT ck_pedidos_costos CHECK (costo_envio >= 0 AND total >= 0),
    CONSTRAINT ck_pedidos_mesa CHECK ((tipo = 'SALON') = (mesa_id IS NOT NULL)),
    CONSTRAINT ck_pedidos_domicilio CHECK (
        tipo <> 'DOMICILIO'
        OR (direccion_entrega IS NOT NULL AND telefono_entrega IS NOT NULL AND metodo_pago IS NOT NULL))
);
CREATE INDEX ix_pedidos_estado_creado ON pedidos (estado, created_at);
CREATE INDEX ix_pedidos_mesero_creado ON pedidos (mesero_id, created_at DESC);
CREATE INDEX ix_pedidos_creado ON pedidos (created_at DESC);
CREATE INDEX ix_pedidos_entregado ON pedidos (entregado_en) WHERE entregado_en IS NOT NULL;
-- Red de seguridad: una mesa no puede tener dos pedidos activos a la vez.
CREATE UNIQUE INDEX ux_pedidos_mesa_activa ON pedidos (mesa_id)
    WHERE mesa_id IS NOT NULL AND estado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO');

CREATE TABLE detalles_pedido (
    id               BIGSERIAL PRIMARY KEY,
    pedido_id        BIGINT         NOT NULL REFERENCES pedidos (id) ON DELETE CASCADE,
    producto_id      BIGINT         NOT NULL REFERENCES productos (id),
    cantidad         INTEGER        NOT NULL,
    precio_unitario  NUMERIC(12, 2) NOT NULL,
    notas            VARCHAR(255),
    CONSTRAINT ck_detalles_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalles_precio CHECK (precio_unitario >= 0)
);
CREATE INDEX ix_detalles_pedido ON detalles_pedido (pedido_id);
CREATE INDEX ix_detalles_producto ON detalles_pedido (producto_id);

CREATE TABLE historial_estados (
    id               BIGSERIAL PRIMARY KEY,
    pedido_id        BIGINT       NOT NULL REFERENCES pedidos (id) ON DELETE CASCADE,
    estado_anterior  VARCHAR(15),
    estado_nuevo     VARCHAR(15)  NOT NULL,
    usuario_id       BIGINT       NOT NULL REFERENCES usuarios (id),
    comentario       VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_historial_pedido ON historial_estados (pedido_id, created_at);
CREATE INDEX ix_historial_estado_fecha ON historial_estados (estado_nuevo, created_at);
