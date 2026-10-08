CREATE DATABASE IF NOT EXISTS restaurante;
USE restaurante;

CREATE TABLE Mesero (
    id_mesero INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255),
    usuario VARCHAR(255),
    contrasena VARCHAR(255),
    PRIMARY KEY (id_mesero)
) ENGINE=InnoDB;

CREATE TABLE PersonalCocina (
    id_cocina INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255),
    usuario VARCHAR(255),
    contrasena VARCHAR(255),
    PRIMARY KEY (id_cocina)
) ENGINE=InnoDB;


CREATE TABLE Administrador (
    id_administrador INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255),
    usuario VARCHAR(255),
    contrasena VARCHAR(255),
    PRIMARY KEY (id_administrador)
) ENGINE=InnoDB;


CREATE TABLE Mesa (
    id_mesa INT NOT NULL AUTO_INCREMENT,
    numero INT,
    estado VARCHAR(255),
    id_administrador INT,
    PRIMARY KEY (id_mesa),
    CONSTRAINT fk_mesa_administrador
        FOREIGN KEY (id_administrador)
        REFERENCES Administrador(id_administrador)
) ENGINE=InnoDB;


CREATE TABLE Categoria (
    id_categoria INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255),
    id_administrador INT,
    PRIMARY KEY (id_categoria),
    CONSTRAINT fk_categoria_administrador
        FOREIGN KEY (id_administrador)
        REFERENCES Administrador(id_administrador)
) ENGINE=InnoDB;


CREATE TABLE Producto (
    id_producto INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255),
    descripcion VARCHAR(255),
    precio INT,
    id_categoria INT,
    id_administrador INT,
    disponibilidad VARCHAR(255),
    PRIMARY KEY (id_producto),
    CONSTRAINT fk_producto_categoria
        FOREIGN KEY (id_categoria)
        REFERENCES Categoria(id_categoria),
    CONSTRAINT fk_producto_administrador
        FOREIGN KEY (id_administrador)
        REFERENCES Administrador(id_administrador)
) ENGINE=InnoDB;


CREATE TABLE Pedido (
    id_pedido INT NOT NULL AUTO_INCREMENT,
    id_mesero INT,
    id_cocina INT,
    id_administrador INT,
    id_mesa INT NULL,
    tipo_pedido VARCHAR(255),
    nombre_cliente VARCHAR(255),
    estado VARCHAR(255),
    total INT,
    metodo_pago VARCHAR(255),
    fecha_creacion VARCHAR(255),
    fecha_cierre VARCHAR(255),
    PRIMARY KEY (id_pedido),
    CONSTRAINT fk_pedido_mesero
        FOREIGN KEY (id_mesero)
        REFERENCES Mesero(id_mesero),
    CONSTRAINT fk_pedido_cocina
        FOREIGN KEY (id_cocina)
        REFERENCES PersonalCocina(id_cocina),
    CONSTRAINT fk_pedido_administrador
        FOREIGN KEY (id_administrador)
        REFERENCES Administrador(id_administrador),
    CONSTRAINT fk_pedido_mesa
        FOREIGN KEY (id_mesa)
        REFERENCES Mesa(id_mesa)
) ENGINE=InnoDB;


CREATE TABLE Domicilio (
    id_domicilio INT NOT NULL AUTO_INCREMENT,
    id_pedido INT UNIQUE,
    direccion VARCHAR(255),
    telefono VARCHAR(255),
    costo_envio INT,
    PRIMARY KEY (id_domicilio),
    CONSTRAINT fk_domicilio_pedido
        FOREIGN KEY (id_pedido)
        REFERENCES Pedido(id_pedido)
) ENGINE=InnoDB;


CREATE TABLE DetallePedido (
    id_detalle INT NOT NULL AUTO_INCREMENT,
    id_pedido INT,
    id_producto INT,
    cantidad INT,
    precio_unitario INT,
    notas VARCHAR(255),
    PRIMARY KEY (id_detalle),
    CONSTRAINT fk_detalle_pedido
        FOREIGN KEY (id_pedido)
        REFERENCES Pedido(id_pedido),
    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (id_producto)
        REFERENCES Producto(id_producto)
) ENGINE=InnoDB;

SHOW TABLES;


SELECT * FROM Mesero;
SELECT * FROM PersonalCocina;
SELECT * FROM Administrador;
SELECT * FROM Mesa;
SELECT * FROM Categoria;
SELECT * FROM Producto;
SELECT * FROM Pedido;
SELECT * FROM Domicilio;
SELECT * FROM DetallePedido;