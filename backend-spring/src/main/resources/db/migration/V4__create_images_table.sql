-- Fotos guardadas en la base de datos (STORAGE_PROVIDER=db). La tabla existe siempre; solo se usa con ese modo.
CREATE TABLE imagenes (
    nombre      VARCHAR(100) PRIMARY KEY,
    tipo        VARCHAR(50)  NOT NULL,
    datos       BYTEA        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
