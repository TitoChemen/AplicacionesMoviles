-- Alias de compatibilidad: contenido actualizado al inicializador completo v5.
-- Preferir init_huerta_escolar_cursos.sql. NO ejecutar ambos: son equivalentes.
-- HUERTA ESCOLAR | INICIALIZACION COMPLETA v5 (cursos, CRUD, historial)
-- Este es el SQL CANONICO para instalaciones nuevas o demos existentes.
-- Si tu base ya funciona, NO necesitas ejecutarlo para actualizar la app;
-- usa migracion_historial_y_admin_v2.sql o las migraciones puntuales.
-- Compatible con MySQL 8 y MariaDB (XAMPP).
-- SOLO DESARROLLO LOCAL: contrasenas de ejemplo en texto plano.
-- Requiere una cuenta administradora de MariaDB para CREATE USER y GRANT.
-- No elimina bases, tablas ni registros existentes.
-- Importar con respaldo previo. Reasigna CURSOS a los usuarios y sectores de DEMO.
-- Tareas alumno/sector de distinto curso quedan OCULTAS hasta corregirse;
-- los nuevos datos demo de este script se crean con cursos coherentes.
-- El backend usuarios-service debe usar app.auth.plaintext-dev=true.
-- IMPORTANTE: Cambiar las credenciales antes de cualquier despliegue real.

-- 1. Bases de datos
CREATE DATABASE IF NOT EXISTS huerta_usuarios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS huerta_cultivos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS huerta_tareas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Cuenta MariaDB utilizada por los microservicios
-- DB_PASSWORD=Huerta2026! en las configuraciones de ejecucion.
CREATE USER IF NOT EXISTS 'huerta_app'@'localhost' IDENTIFIED BY 'Huerta2026!';
ALTER USER 'huerta_app'@'localhost' IDENTIFIED BY 'Huerta2026!';
GRANT ALL PRIVILEGES ON huerta_usuarios.* TO 'huerta_app'@'localhost';
GRANT ALL PRIVILEGES ON huerta_cultivos.* TO 'huerta_app'@'localhost';
GRANT ALL PRIVILEGES ON huerta_tareas.* TO 'huerta_app'@'localhost';

-- 3. Usuarios
USE huerta_usuarios;
CREATE TABLE IF NOT EXISTS usuarios (
  id BIGINT NOT NULL AUTO_INCREMENT,
  activo BIT(1) NOT NULL,
  contrasena_hash VARCHAR(255) NOT NULL,
  correo VARCHAR(255) NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  rol ENUM('ADMINISTRADOR','DOCENTE','ESTUDIANTE') NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_usuarios_correo (correo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 3A. Catalogo academico y migracion de la tabla existente.
CREATE TABLE IF NOT EXISTS huerta_usuarios.cursos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  codigo VARCHAR(20) NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cursos_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO huerta_usuarios.cursos (codigo,nombre) VALUES
('7-A','Septimo Basico A'),('7-B','Septimo Basico B'),('8-A','Octavo Basico A')
ON DUPLICATE KEY UPDATE codigo=VALUES(codigo);

SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_usuarios.usuarios ADD COLUMN curso_id BIGINT NULL',
 'DO 0')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_usuarios' AND TABLE_NAME='usuarios' AND COLUMN_NAME='curso_id');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

-- Actualizar rol de cuentas ya existentes, preservando datos anteriores.
-- ADMINISTRADOR es global (curso_id NULL); DOCENTE tiene curso obligatorio.
ALTER TABLE huerta_usuarios.usuarios
  MODIFY COLUMN rol ENUM('ADMINISTRADOR','DOCENTE','ESTUDIANTE') NOT NULL;

-- SOLO DEMO: estas dos cuentas se restablecen al ejecutar el script.
-- Las demas conservan su contrasena previa al reimportar; ver Cuentas.txt.
INSERT INTO usuarios (activo, contrasena_hash, correo, nombre, rol)
VALUES (b'1','Profesor123!','profesor@huerta.cl','Profesor Juan','DOCENTE')
ON DUPLICATE KEY UPDATE activo=VALUES(activo), contrasena_hash=VALUES(contrasena_hash), nombre=VALUES(nombre), rol=VALUES(rol);
INSERT INTO usuarios (activo, contrasena_hash, correo, nombre, rol)
VALUES (b'1','Estudiante123!','estudiante@huerta.cl','Kevin Estudiante','ESTUDIANTE')
ON DUPLICATE KEY UPDATE activo=VALUES(activo), contrasena_hash=VALUES(contrasena_hash), nombre=VALUES(nombre), rol=VALUES(rol);

-- Colaboradores ficticios: si ya existen, se preservan sus datos y contrasenas.
INSERT INTO usuarios (activo, contrasena_hash, correo, nombre, rol) VALUES
(b'1','Docente2026!','maria.docente@huerta.cl','Maria Gonzalez','DOCENTE'),
(b'1','Docente2026!','pedro.docente@huerta.cl','Pedro Ramirez','DOCENTE'),
(b'1','Estudiante2026!','camila@huerta.cl','Camila Rojas','ESTUDIANTE'),
(b'1','Estudiante2026!','matias@huerta.cl','Matias Soto','ESTUDIANTE'),
(b'1','Estudiante2026!','valentina@huerta.cl','Valentina Perez','ESTUDIANTE'),
(b'1','Estudiante2026!','diego@huerta.cl','Diego Morales','ESTUDIANTE'),
(b'1','Estudiante2026!','sofia@huerta.cl','Sofia Herrera','ESTUDIANTE'),
(b'1','Estudiante2026!','benjamin@huerta.cl','Benjamin Torres','ESTUDIANTE')
ON DUPLICATE KEY UPDATE id=id;

-- Docente adicional del 7-B para pruebas cruzadas y administrador sin curso.
INSERT INTO huerta_usuarios.usuarios (activo,contrasena_hash,correo,nombre,rol)
VALUES (b'1','Profesor7B2026!','profesor.7b@huerta.cl','Profesor Gabriel 7-B','DOCENTE')
ON DUPLICATE KEY UPDATE id=id;
INSERT INTO huerta_usuarios.usuarios (activo,contrasena_hash,correo,nombre,rol)
VALUES (b'1','AdminGlobal2026!','admin.global@huerta.cl','Administradora General','ADMINISTRADOR')
ON DUPLICATE KEY UPDATE id=id;

-- Migrar SOLO los docentes de demostracion antes marcados ADMINISTRADOR.
-- Nunca degradar administradores reales que no pertenezcan a las cuentas de demo.
UPDATE huerta_usuarios.usuarios
SET rol='DOCENTE'
WHERE rol='ADMINISTRADOR' AND correo IN
 ('profesor@huerta.cl','maria.docente@huerta.cl',
  'pedro.docente@huerta.cl','profesor.7b@huerta.cl');
UPDATE huerta_usuarios.usuarios
SET rol='ADMINISTRADOR', curso_id=NULL, activo=b'1'
WHERE correo='admin.global@huerta.cl';

-- Docentes: profesor Juan = 7-A; Maria = 7-B; Pedro = 8-A.
-- Estudiantes ficticios se distribuyen entre esos cursos.
UPDATE huerta_usuarios.usuarios u JOIN huerta_usuarios.cursos c
  ON c.codigo = CASE
      WHEN u.correo IN ('profesor@huerta.cl','estudiante@huerta.cl','valentina@huerta.cl') THEN '7-A'
      WHEN u.correo IN ('maria.docente@huerta.cl','profesor.7b@huerta.cl','camila@huerta.cl','matias@huerta.cl','sofia@huerta.cl') THEN '7-B'
      WHEN u.correo IN ('pedro.docente@huerta.cl','diego@huerta.cl','benjamin@huerta.cl') THEN '8-A'
      ELSE NULL END
SET u.curso_id=c.id;

-- Garantizar que la cuenta global no quede vinculada a un curso.
UPDATE huerta_usuarios.usuarios SET curso_id=NULL WHERE correo='admin.global@huerta.cl';

-- 4. Sectores y cultivos
USE huerta_cultivos;
CREATE TABLE IF NOT EXISTS sectores (
  id BIGINT NOT NULL AUTO_INCREMENT,
  codigo VARCHAR(255) NOT NULL,
  descripcion VARCHAR(1000) DEFAULT NULL,
  estado VARCHAR(255) NOT NULL,
  fecha_creacion VARCHAR(255) DEFAULT NULL,
  nombre VARCHAR(255) NOT NULL,
  responsable_id VARCHAR(255) DEFAULT NULL,
  ubicacion VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sectores_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cultivos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  descripcion VARCHAR(1000) DEFAULT NULL,
  estado_desarrollo VARCHAR(255) DEFAULT NULL,
  fecha_cosecha_estimada VARCHAR(255) DEFAULT NULL,
  fecha_siembra VARCHAR(255) DEFAULT NULL,
  frecuencia_riego VARCHAR(255) DEFAULT NULL,
  nombre_comun VARCHAR(255) NOT NULL,
  sector_id BIGINT NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 4A. Agregar asociacion del sector al curso en instalaciones antiguas.
SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_cultivos.sectores ADD COLUMN curso_id BIGINT NULL',
 'DO 0')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_cultivos' AND TABLE_NAME='sectores' AND COLUMN_NAME='curso_id');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

INSERT INTO sectores (codigo,descripcion,estado,fecha_creacion,nombre,responsable_id,ubicacion)
SELECT x.codigo,x.descripcion,'ACTIVO','2026-11-09',x.nombre,CAST(u.id AS CHAR),x.ubicacion
FROM (
 SELECT 'SEC-001' codigo,'Almacigos y hortalizas de hoja' descripcion,'Huerto Principal' nombre,'Patio norte' ubicacion,'profesor@huerta.cl' responsable
 UNION ALL SELECT 'SEC-002','Lechugas y hierbas','Huerto Secundario','Patio sur','profesor@huerta.cl'
 UNION ALL SELECT 'SEC-003','Hierbas aromaticas y medicinales','Jardin Aromatico','Patio este','maria.docente@huerta.cl'
 UNION ALL SELECT 'SEC-004','Hortalizas de raiz','Huerto de Raices','Patio oeste','pedro.docente@huerta.cl'
 UNION ALL SELECT 'SEC-005','Plantas de temporada','Invernadero Escolar','Invernadero','maria.docente@huerta.cl'
) x
JOIN huerta_usuarios.usuarios u ON u.correo=x.responsable
WHERE NOT EXISTS (SELECT 1 FROM sectores s WHERE s.codigo=x.codigo);


-- Cada sector pertence a un unico curso.
UPDATE huerta_cultivos.sectores s JOIN huerta_usuarios.cursos c
  ON c.codigo = CASE
      WHEN s.codigo IN ('SEC-001','SEC-002') THEN '7-A'
      WHEN s.codigo IN ('SEC-003','SEC-005') THEN '7-B'
      WHEN s.codigo IN ('SEC-004') THEN '8-A'
      ELSE NULL END
SET s.curso_id=c.id
WHERE s.curso_id IS NULL; -- Nunca pisar cursos elegidos luego por el administrador.

INSERT INTO cultivos (descripcion,estado_desarrollo,fecha_cosecha_estimada,fecha_siembra,frecuencia_riego,nombre_comun,sector_id)
SELECT x.descripcion,x.estado,x.cosecha,x.siembra,x.riego,x.nombre,s.id
FROM (
 SELECT 'SEC-001' codigo,'Tomate Cherry' nombre,'Cultivo de tomates cherry' descripcion,'CRECIMIENTO' estado,'2027-02-15' cosecha,'2026-11-09' siembra,'Diario' riego
 UNION ALL SELECT 'SEC-001','Acelga','Hortaliza de hoja verde','CRECIMIENTO','2027-01-20','2026-11-10','Cada 2 dias'
 UNION ALL SELECT 'SEC-002','Lechuga','Cultivo de lechugas','CRECIMIENTO','2027-01-10','2026-11-09','Cada 2 dias'
 UNION ALL SELECT 'SEC-002','Espinaca','Cultivo de espinacas','SIEMBRA','2027-01-25','2026-11-12','Cada 2 dias'
 UNION ALL SELECT 'SEC-003','Albahaca','Hierba aromatica','CRECIMIENTO','2027-01-30','2026-11-09','Diario'
 UNION ALL SELECT 'SEC-003','Menta','Hierba aromatica','CRECIMIENTO','2027-02-01','2026-11-11','Cada 2 dias'
 UNION ALL SELECT 'SEC-004','Zanahoria','Hortaliza de raiz','SIEMBRA','2027-03-01','2026-11-10','Cada 2 dias'
 UNION ALL SELECT 'SEC-004','Rabano','Hortaliza de ciclo corto','CRECIMIENTO','2026-12-15','2026-11-10','Cada 2 dias'
 UNION ALL SELECT 'SEC-005','Pimenton','Hortaliza de invernadero','CRECIMIENTO','2027-03-15','2026-11-11','Diario'
 UNION ALL SELECT 'SEC-005','Pepino','Hortaliza de invernadero','SIEMBRA','2027-02-20','2026-11-12','Diario'
) x JOIN sectores s ON s.codigo=x.codigo
WHERE NOT EXISTS (SELECT 1 FROM cultivos c WHERE c.sector_id=s.id AND c.nombre_comun=x.nombre);

-- 5. Tareas y alertas
USE huerta_tareas;
CREATE TABLE IF NOT EXISTS alertas (
  id BIGINT NOT NULL AUTO_INCREMENT,
  descripcion VARCHAR(2000) NOT NULL,
  estado VARCHAR(255) NOT NULL,
  fecha_reporte VARCHAR(255) DEFAULT NULL,
  sector_id BIGINT NOT NULL,
  tipo VARCHAR(255) NOT NULL,
  usuario_id BIGINT DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tareas (
  id BIGINT NOT NULL AUTO_INCREMENT,
  descripcion VARCHAR(2000) DEFAULT NULL,
  estado ENUM('APROBADA','EN_PROGRESO','PENDIENTE','PENDIENTE_VALIDACION','RECHAZADA') NOT NULL,
  estudiante_id BIGINT DEFAULT NULL,
  evidencia_url VARCHAR(1000) DEFAULT NULL,
  comentario_estudiante VARCHAR(1000) DEFAULT NULL,
  fecha_asignacion VARCHAR(255) DEFAULT NULL,
  fecha_vencimiento VARCHAR(255) DEFAULT NULL,
  observacion_docente VARCHAR(1000) DEFAULT NULL,
  sector_id BIGINT NOT NULL,
  titulo VARCHAR(255) NOT NULL,
  curso_id BIGINT DEFAULT NULL,
  intento_actual INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agregar comentario_estudiante a instalaciones anteriores, si falta (MariaDB/MySQL compatible).
SET @add_comentario = (
 SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_tareas.tareas ADD COLUMN comentario_estudiante VARCHAR(1000) DEFAULT NULL',
 'SELECT ''comentario_estudiante ya existe'' AS informacion')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_tareas' AND TABLE_NAME='tareas' AND COLUMN_NAME='comentario_estudiante'
);
PREPARE stmt FROM @add_comentario;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 5A. Asociacion curso a tareas y alertas; esquema compatible con instalaciones anteriores.
SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_tareas.tareas ADD COLUMN curso_id BIGINT NULL',
 'DO 0')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_tareas' AND TABLE_NAME='tareas' AND COLUMN_NAME='curso_id');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_tareas.alertas ADD COLUMN curso_id BIGINT NULL',
 'DO 0')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_tareas' AND TABLE_NAME='alertas' AND COLUMN_NAME='curso_id');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

-- Historial persistente: compatible con entidades Tarea e HistorialTarea.
-- Evitar ADD COLUMN IF NOT EXISTS: algunos MySQL 8 no lo soportan.
SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_tareas.tareas ADD COLUMN intento_actual INT NOT NULL DEFAULT 0',
 'DO 0')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_tareas' AND TABLE_NAME='tareas' AND COLUMN_NAME='intento_actual');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;
CREATE TABLE IF NOT EXISTS huerta_tareas.tarea_historial (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tarea_id BIGINT NOT NULL,
  intento INT NOT NULL,
  accion VARCHAR(32) NOT NULL,
  actor_id BIGINT NOT NULL,
  comentario VARCHAR(1000) NULL,
  evidencia_url VARCHAR(1000) NULL,
  fecha DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_historial_tarea_fecha (tarea_id, fecha, id),
  CONSTRAINT fk_historial_tarea FOREIGN KEY (tarea_id) REFERENCES huerta_tareas.tareas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tareas (descripcion,estado,estudiante_id,evidencia_url,comentario_estudiante,fecha_asignacion,fecha_vencimiento,observacion_docente,sector_id,titulo)
SELECT x.descripcion,x.estado,u.id,NULL,NULL,'2026-11-09',x.vencimiento,NULL,s.id,x.titulo
FROM (
 SELECT 'SEC-001' codigo,'estudiante@huerta.cl' correo,'Riego de tomates' titulo,'Regar los almacigos de tomates y revisar la humedad' descripcion,'PENDIENTE' estado,'2026-11-10' vencimiento
 UNION ALL SELECT 'SEC-002','estudiante@huerta.cl','Revision de lechugas','Revisar el desarrollo de las lechugas','PENDIENTE','2026-11-12'
 UNION ALL SELECT 'SEC-003','camila@huerta.cl','Riego de albahaca','Regar albahaca y revisar hojas','PENDIENTE','2026-11-18'
 UNION ALL SELECT 'SEC-003','matias@huerta.cl','Control de menta','Revisar humedad y crecimiento de menta','EN_PROGRESO','2026-11-19'
 UNION ALL SELECT 'SEC-004','diego@huerta.cl','Siembra de zanahorias','Preparar surcos y sembrar zanahorias','PENDIENTE','2026-11-20'
 UNION ALL SELECT 'SEC-004','diego@huerta.cl','Revision de rabanos','Controlar germinacion de rabanos','PENDIENTE_VALIDACION','2026-11-16'
 UNION ALL SELECT 'SEC-005','sofia@huerta.cl','Ventilacion del invernadero','Comprobar ventilacion y temperatura','EN_PROGRESO','2026-11-17'
 UNION ALL SELECT 'SEC-005','benjamin@huerta.cl','Riego de pepinos','Regar pepinos y registrar observaciones','PENDIENTE','2026-11-19'
 UNION ALL SELECT 'SEC-001','valentina@huerta.cl','Control de plagas tomates','Inspeccionar hojas de tomate en busca de plagas','PENDIENTE','2026-11-21'
 UNION ALL SELECT 'SEC-002','valentina@huerta.cl','Cosecha de hojas','Revisar hojas listas para cosecha','APROBADA','2026-11-15'
) x
JOIN huerta_usuarios.usuarios u ON u.correo=x.correo
JOIN huerta_cultivos.sectores s ON s.codigo=x.codigo
WHERE NOT EXISTS (SELECT 1 FROM tareas t WHERE t.titulo=x.titulo AND t.sector_id=s.id);

INSERT INTO alertas (descripcion,estado,fecha_reporte,sector_id,tipo,usuario_id)
SELECT x.descripcion,x.estado,'2026-11-09',s.id,x.tipo,u.id
FROM (
 SELECT 'SEC-001' codigo,'estudiante@huerta.cl' correo,'Revisar humedad del sector principal' descripcion,'PENDIENTE' estado,'RIEGO' tipo
 UNION ALL SELECT 'SEC-002','estudiante@huerta.cl','Inspeccionar hojas de lechuga','PENDIENTE','REVISION'
 UNION ALL SELECT 'SEC-003','camila@huerta.cl','Controlar riego de hierbas aromaticas','PENDIENTE','RIEGO'
 UNION ALL SELECT 'SEC-004','diego@huerta.cl','Revisar germinacion de zanahorias','PENDIENTE','REVISION'
 UNION ALL SELECT 'SEC-005','sofia@huerta.cl','Verificar temperatura del invernadero','PENDIENTE','TEMPERATURA'
) x
JOIN huerta_cultivos.sectores s ON s.codigo=x.codigo
JOIN huerta_usuarios.usuarios u ON u.correo=x.correo
WHERE NOT EXISTS (SELECT 1 FROM alertas a WHERE a.sector_id=s.id AND a.descripcion=x.descripcion);


-- 5B. Migracion de tareas existentes.
-- Si un alumno y un sector no pertenecen al mismo curso, NO le asignamos curso:
-- se oculta la tarea inconsistente hasta corregirla manualmente.
UPDATE huerta_tareas.tareas t
LEFT JOIN huerta_usuarios.usuarios u ON u.id=t.estudiante_id
LEFT JOIN huerta_cultivos.sectores s ON s.id=t.sector_id
SET t.curso_id = IF(u.curso_id <=> s.curso_id, u.curso_id, NULL)
WHERE t.curso_id IS NULL;
UPDATE huerta_tareas.alertas a
LEFT JOIN huerta_usuarios.usuarios u ON u.id=a.usuario_id
LEFT JOIN huerta_cultivos.sectores s ON s.id=a.sector_id
SET a.curso_id = IF(u.curso_id <=> s.curso_id, u.curso_id, NULL)
WHERE a.curso_id IS NULL;

-- Ejemplos adicionales por curso, para demostrar aislamiento.
INSERT INTO huerta_tareas.tareas
 (descripcion, estado, estudiante_id, evidencia_url, comentario_estudiante,
  fecha_asignacion, fecha_vencimiento, observacion_docente, sector_id, titulo, curso_id)
SELECT demo.descripcion,'PENDIENTE',u.id,NULL,NULL,'2026-11-16','2026-11-30',NULL,s.id,demo.titulo,c.id
FROM (
 SELECT '7-A' curso, 'SEC-001' codigo, 'estudiante@huerta.cl' correo,
        'Informe de germinacion 7-A' titulo, 'Registrar el avance del semillero del 7-A' descripcion
 UNION ALL SELECT '7-A','SEC-002','valentina@huerta.cl','Riego de lechugas 7-A','Regar lechugas del curso 7-A'
 UNION ALL SELECT '7-B','SEC-003','camila@huerta.cl','Informe de germinacion 7-B','Registrar el avance del semillero del 7-B'
 UNION ALL SELECT '7-B','SEC-005','sofia@huerta.cl','Riego del invernadero 7-B','Comprobar plantas del invernadero 7-B'
 UNION ALL SELECT '8-A','SEC-004','diego@huerta.cl','Registro de zanahorias 8-A','Medir crecimiento del cultivo del 8-A'
) demo
JOIN huerta_usuarios.cursos c ON c.codigo=demo.curso
JOIN huerta_usuarios.usuarios u ON u.correo=demo.correo AND u.curso_id=c.id
JOIN huerta_cultivos.sectores s ON s.codigo=demo.codigo AND s.curso_id=c.id
WHERE NOT EXISTS (SELECT 1 FROM huerta_tareas.tareas t WHERE t.titulo=demo.titulo AND t.sector_id=s.id);

-- Ejemplos de justificaciones para verificar la vista docente.
-- Son demostraciones textuales, NO fotografías reales. No se alteran evidencias subidas.
UPDATE huerta_tareas.tareas
SET comentario_estudiante='Regué las plantas y registré la humedad del 7-A para la revisión docente.',
    estado='PENDIENTE_VALIDACION'
WHERE titulo='Informe de germinacion 7-A'
  AND comentario_estudiante IS NULL AND evidencia_url IS NULL;
UPDATE huerta_tareas.tareas
SET comentario_estudiante='Revisé el crecimiento de las semillas del 7-B y anoté los cambios del día.',
    estado='PENDIENTE_VALIDACION'
WHERE titulo='Informe de germinacion 7-B'
  AND comentario_estudiante IS NULL AND evidencia_url IS NULL;

-- Control de integridad: revisar filas con curso_id NULL o matriculas inconsistentes.
SELECT t.id,t.titulo,t.estudiante_id,t.sector_id FROM huerta_tareas.tareas t
WHERE t.curso_id IS NULL;
SELECT a.id,a.descripcion,a.usuario_id,a.sector_id FROM huerta_tareas.alertas a
WHERE a.curso_id IS NULL;

-- 6. Comprobacion de datos
SELECT 'cursos' AS tabla,COUNT(*) AS registros FROM huerta_usuarios.cursos
UNION ALL SELECT 'usuarios',COUNT(*) FROM huerta_usuarios.usuarios
UNION ALL SELECT 'sectores',COUNT(*) FROM huerta_cultivos.sectores
UNION ALL SELECT 'cultivos',COUNT(*) FROM huerta_cultivos.cultivos
UNION ALL SELECT 'tareas',COUNT(*) FROM huerta_tareas.tareas
UNION ALL SELECT 'alertas',COUNT(*) FROM huerta_tareas.alertas;

-- SOLO DESARROLLO: consultar credenciales de demostracion.
SELECT nombre,correo,contrasena_hash AS contrasena_de_prueba,rol
FROM huerta_usuarios.usuarios
WHERE correo LIKE '%@huerta.cl'
ORDER BY rol,correo;

-- Resumen por curso de demostracion
SELECT c.codigo AS curso, u.nombre AS docente,
 (SELECT COUNT(*) FROM huerta_usuarios.usuarios a WHERE a.curso_id=c.id AND a.rol='ESTUDIANTE') AS estudiantes,
 (SELECT COUNT(*) FROM huerta_tareas.tareas t WHERE t.curso_id=c.id) AS tareas
FROM huerta_usuarios.cursos c
JOIN huerta_usuarios.usuarios u ON u.curso_id=c.id AND u.rol='DOCENTE'
ORDER BY c.codigo;

-- Validacion de rol y curso de todos los usuarios de demo.
SELECT u.correo,u.nombre,u.rol,COALESCE(c.codigo,'TODOS') AS curso
FROM huerta_usuarios.usuarios u
LEFT JOIN huerta_usuarios.cursos c ON c.id=u.curso_id
WHERE u.correo IN ('admin.global@huerta.cl','profesor@huerta.cl',
  'profesor.7b@huerta.cl','maria.docente@huerta.cl','estudiante@huerta.cl',
  'camila@huerta.cl','pedro.docente@huerta.cl','diego@huerta.cl',
  'benjamin@huerta.cl','valentina@huerta.cl','matias@huerta.cl','sofia@huerta.cl')
ORDER BY u.rol,u.correo;

-- Justificaciones incluidas en el backend, visibles solo a docentes autorizados o admin global.
SELECT t.id,t.titulo,t.comentario_estudiante,t.curso_id
FROM huerta_tareas.tareas t
WHERE t.comentario_estudiante IS NOT NULL;
