-- Datos de ejemplo para mostrar el menú (sin fotos: se suben desde el panel del administrador).
-- Ejecutar UNA vez, en el editor SQL de Neon, DESPUÉS de que el backend haya arrancado por primera vez
-- (así las tablas ya existen). Es seguro repetirlo: no duplica lo que ya existe.

INSERT INTO categorias (nombre, descripcion) VALUES
    ('Hamburguesas', 'Carne al carbón y pan artesanal'),
    ('Platos fuertes', 'Para los más hambrientos'),
    ('Entradas', 'Para compartir'),
    ('Bebidas', 'Naturales y gaseosas'),
    ('Postres', 'El final dulce')
ON CONFLICT DO NOTHING;

INSERT INTO productos (categoria_id, nombre, descripcion, precio)
SELECT c.id, p.nombre, p.descripcion, p.precio
FROM (VALUES
    ('Hamburguesas', 'Clásica',          '150 g de carne, queso, lechuga, tomate y salsa de la casa', 18500),
    ('Hamburguesas', 'Doble queso',      'Doble carne, doble queso cheddar y cebolla caramelizada',   24500),
    ('Hamburguesas', 'BBQ Crispy',       'Carne, tocineta, aros de cebolla y salsa BBQ ahumada',      26500),
    ('Platos fuertes', 'Parrillada mixta', 'Res, pollo y chorizo con papas criollas y arepa',          52000),
    ('Platos fuertes', 'Pechuga a la plancha', 'Con arroz, ensalada fresca y patacón',                 28000),
    ('Entradas', 'Nachos con guacamole', 'Totopos, queso fundido, pico de gallo y guacamole',          16000),
    ('Entradas', 'Alitas BBQ x8',        'Alitas crocantes bañadas en salsa BBQ',                      21000),
    ('Bebidas', 'Limonada de coco',      'Cremosa y bien fría',                                         9500),
    ('Bebidas', 'Jugo natural',          'Mora, maracuyá o lulo, en agua o en leche',                   8000),
    ('Postres', 'Brownie con helado',    'Brownie tibio con helado de vainilla',                       13500)
) AS p (categoria, nombre, descripcion, precio)
JOIN categorias c ON c.nombre = p.categoria
ON CONFLICT DO NOTHING;

INSERT INTO mesas (numero, capacidad) VALUES (1, 2), (2, 4), (3, 4), (4, 6), (5, 6), (6, 8)
ON CONFLICT DO NOTHING;
