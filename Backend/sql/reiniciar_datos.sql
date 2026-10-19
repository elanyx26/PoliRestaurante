-- Borra TODOS los datos (conserva las tablas). Útil para limpiar los datos de la demo.
USE restaurante;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE HistorialEstado;   -- existe después de la primera ejecución de la aplicación
TRUNCATE TABLE DetallePedido;
TRUNCATE TABLE Domicilio;
TRUNCATE TABLE Pedido;
TRUNCATE TABLE Producto;
TRUNCATE TABLE Categoria;
TRUNCATE TABLE Mesa;
TRUNCATE TABLE Administrador;
TRUNCATE TABLE Mesero;
TRUNCATE TABLE PersonalCocina;
SET FOREIGN_KEY_CHECKS = 1;
