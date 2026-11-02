-- Configuración inicial de pagos a domicilio: máximo de $80.000 para contraentrega.
INSERT INTO configuracion_domicilios (limite_contraentrega, activa)
SELECT 80000.00, TRUE
WHERE NOT EXISTS (SELECT 1 FROM configuracion_domicilios WHERE activa);
