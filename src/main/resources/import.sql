-- ============================================================
-- import.sql — Datos iniciales de BiblioBox
-- Spring Boot ejecuta este fichero automáticamente al arrancar
-- cuando spring.jpa.hibernate.ddl-auto=create o create-drop.
-- Las tablas ya existen en ese momento (Hibernate las creó primero).
-- ============================================================

-- Alumnos
INSERT INTO alumnos (nombre, curso, email) VALUES ('Ana García',      '1ºESO-A', 'ana.garcia@colegio.es');
INSERT INTO alumnos (nombre, curso, email) VALUES ('Carlos López',    '1ºESO-B', 'carlos.lopez@colegio.es');
INSERT INTO alumnos (nombre, curso, email) VALUES ('María Martínez',  '2ºESO-A', 'maria.martinez@colegio.es');
INSERT INTO alumnos (nombre, curso, email) VALUES ('Pedro Sánchez',   '2ºESO-B', 'pedro.sanchez@colegio.es');
INSERT INTO alumnos (nombre, curso, email) VALUES ('Lucía Fernández', '3ºESO-A', 'lucia.fernandez@colegio.es');

-- Préstamos (alumno_id referencia el orden de inserción anterior: 1=Ana, 2=Carlos, 3=María, 4=Pedro, 5=Lucía)

-- Ana: 2 devueltos, 1 pendiente
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('El Quijote',           '2025-09-10', true,  '2025-09-20', 1);
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('Cien años de soledad', '2025-10-05', true,  '2025-10-15', 1);
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('El principito',        '2026-01-08', false, null,         1);

-- Carlos: 1 devuelto, 1 pendiente
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('Harry Potter',         '2025-10-12', true,  '2025-10-25', 2);
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('La Odisea',            '2025-11-03', false, null,         2);

-- María: 3 devueltos, 1 pendiente
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('Romeo y Julieta',      '2025-09-15', true,  '2025-09-28', 3);
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('1984',                 '2025-10-20', true,  '2025-11-02', 3);
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('Matar a un ruiseñor',  '2025-11-10', true,  '2025-11-22', 3);
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('El gran Gatsby',       '2026-01-15', false, null,         3);

-- Pedro: 1 devuelto
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('Crimen y castigo',     '2025-12-05', true,  '2025-12-18', 4);

-- Lucía: 1 devuelto, 1 pendiente
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('Orgullo y prejuicio',  '2025-11-20', true,  '2025-12-03', 5);
INSERT INTO prestamos (titulo_libro, fecha_prestamo, devuelto, fecha_devolucion, alumno_id) VALUES ('La metamorfosis',      '2026-02-01', false, null,         5);
