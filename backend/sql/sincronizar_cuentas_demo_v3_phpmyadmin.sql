-- VERSION PHPMyAdmin: funciona aun si esta seleccionada huerta_cultivos.
-- Ejecuta el archivo entero en SQL o Importar; esquema huerta_usuarios debe existir.
-- HUERTA ESCOLAR | SINCRONIZAR SOLO CUENTAS DE DEMO
-- Ejecutar SOLO con esquema huerta_usuarios.cursos / usuarios ya migrado.
-- Crea cuentas faltantes y corrige curso/rol de las 12 cuentas DEMO.
-- NO sobreescribe la contrasena ni el nombre de cuentas existentes.
-- NO toca tareas, cultivos, fotografias ni sectores.
-- Para nuevas cuentas, estas claves requieren PLAINTEXT_DEV=true (solo demo).
-- No requiere USE: todas las tablas usan huerta_usuarios.<tabla>.
INSERT INTO huerta_usuarios.cursos (codigo, nombre) VALUES
('7-A','Septimo Basico A'),('7-B','Septimo Basico B'),('8-A','Octavo Basico A')
ON DUPLICATE KEY UPDATE codigo=VALUES(codigo);

INSERT INTO huerta_usuarios.usuarios (activo, contrasena_hash, correo, nombre, rol, curso_id)
SELECT b'1', d.clave, d.correo, d.nombre, d.rol, c.id FROM (
 SELECT 'profesor@huerta.cl' correo, 'Profesor Juan' nombre, 'Profesor123!' clave, 'DOCENTE' rol, '7-A' codigo
 UNION ALL SELECT 'profesor.7b@huerta.cl','Profesor Gabriel 7-B','Profesor7B2026!','DOCENTE','7-B'
 UNION ALL SELECT 'maria.docente@huerta.cl','Maria Gonzalez','Docente2026!','DOCENTE','7-B'
 UNION ALL SELECT 'pedro.docente@huerta.cl','Pedro Ramirez','Docente2026!','DOCENTE','8-A'
 UNION ALL SELECT 'estudiante@huerta.cl','Kevin Estudiante','Estudiante123!','ESTUDIANTE','7-A'
 UNION ALL SELECT 'valentina@huerta.cl','Valentina Perez','Estudiante2026!','ESTUDIANTE','7-A'
 UNION ALL SELECT 'camila@huerta.cl','Camila Rojas','Estudiante2026!','ESTUDIANTE','7-B'
 UNION ALL SELECT 'matias@huerta.cl','Matias Soto','Estudiante2026!','ESTUDIANTE','7-B'
 UNION ALL SELECT 'sofia@huerta.cl','Sofia Herrera','Estudiante2026!','ESTUDIANTE','7-B'
 UNION ALL SELECT 'diego@huerta.cl','Diego Morales','Estudiante2026!','ESTUDIANTE','8-A'
 UNION ALL SELECT 'benjamin@huerta.cl','Benjamin Torres','Estudiante2026!','ESTUDIANTE','8-A'
) d JOIN huerta_usuarios.cursos c ON c.codigo=d.codigo
ON DUPLICATE KEY UPDATE rol=VALUES(rol),curso_id=VALUES(curso_id);

INSERT INTO huerta_usuarios.usuarios (activo, contrasena_hash, correo, nombre, rol, curso_id)
VALUES (b'1','AdminGlobal2026!','admin.global@huerta.cl','Administradora General','ADMINISTRADOR',NULL)
ON DUPLICATE KEY UPDATE rol=VALUES(rol),curso_id=NULL;

SELECT u.nombre,u.correo,u.rol,COALESCE(c.codigo,'TODOS') AS curso,
       IF(u.activo=b'1','ACTIVA','INACTIVA') AS estado
FROM huerta_usuarios.usuarios u LEFT JOIN huerta_usuarios.cursos c ON c.id=u.curso_id
WHERE u.correo IN (
 'admin.global@huerta.cl','profesor@huerta.cl','profesor.7b@huerta.cl',
 'maria.docente@huerta.cl','pedro.docente@huerta.cl','estudiante@huerta.cl',
 'valentina@huerta.cl','camila@huerta.cl','matias@huerta.cl',
 'sofia@huerta.cl','diego@huerta.cl','benjamin@huerta.cl')
ORDER BY c.codigo,u.rol,u.nombre;
