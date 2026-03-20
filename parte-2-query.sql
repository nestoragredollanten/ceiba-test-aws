SELECT DISTINCT c.nombre
FROM cliente c
WHERE c.id IN (
    SELECT i.idCliente
    FROM inscripcion i
             JOIN disponibilidad d ON d.idProducto = i.idProducto
             JOIN visitan v ON v.idCliente = i.idCliente AND v.idSucursal = d.idSucursal
    GROUP BY i.idCliente, i.idProducto
    HAVING COUNT(DISTINCT d.idSucursal) = COUNT(DISTINCT v.idSucursal)
);