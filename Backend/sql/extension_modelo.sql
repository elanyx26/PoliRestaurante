-- OPCIONAL: el programa ejecuta esto automáticamente al conectarse por primera vez
-- (util/Conexion.java), solo si los elementos no existen. Este archivo es únicamente
-- documentación / alternativa manual. Ejecútelo UNA sola vez, antes de abrir la aplicación.
USE restaurante;

-- HU19: activar / desactivar usuarios (conservan su historial)
ALTER TABLE Mesero         ADD COLUMN activo TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE PersonalCocina ADD COLUMN activo TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE Administrador  ADD COLUMN activo TINYINT(1) NOT NULL DEFAULT 1;

-- HU3: un pedido confirmado y enviado a cocina ya no se puede editar
ALTER TABLE Pedido ADD COLUMN enviado_cocina TINYINT(1) NOT NULL DEFAULT 0;

-- RF-15 / RNF-09 / HU4 / HU20: fecha y hora de cada cambio de estado del pedido
CREATE TABLE IF NOT EXISTS HistorialEstado (
    id_historial INT NOT NULL AUTO_INCREMENT,
    id_pedido INT NOT NULL,
    estado VARCHAR(255),
    fecha VARCHAR(255),
    PRIMARY KEY (id_historial),
    CONSTRAINT fk_historial_pedido FOREIGN KEY (id_pedido) REFERENCES Pedido(id_pedido)
) ENGINE=InnoDB;
