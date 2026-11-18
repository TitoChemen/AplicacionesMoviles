-- HUERTA ESCOLAR | INICIALIZADOR Y MIGRACION UNIFICADOS (MySQL 8 / MariaDB-XAMPP)
-- Ejecutar UNA VEZ completo (Importar en phpMyAdmin o mysql -u root -p < archivo.sql).
-- Se puede repetir: crea solo lo que falte y conserva datos y contrasenas existentes.
-- Usar una cuenta BD con permisos CREATE DATABASE, CREATE USER, ALTER y GRANT.
-- Funciona aunque phpMyAdmin tenga seleccionada otra base; todo usa nombres completos.
-- SOLO PARA DEMO LOCAL: las claves de cuentas nuevas son texto plano (PLAINTEXT_DEV=true).
-- Conservar un respaldo antes de usarlo en una BD existente. No hace DROP/TRUNCATE/DELETE.
-- Se corrigen rol y curso de las 12 cuentas de DEMO; no se cambia su nombre, clave o activo.
-- Las tareas/alertas antiguas con cursos incoherentes quedan con curso_id NULL (invisibles
-- para alumnos/docentes) hasta corregir la relacion; no se borran ni se cambia su historial.

-- 1. Esquemas y cuenta de los microservicios. No se restablece su clave si ya existe.
CREATE DATABASE IF NOT EXISTS huerta_usuarios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS huerta_cultivos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS huerta_tareas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'huerta_app'@'localhost' IDENTIFIED BY 'Huerta2026!';
GRANT ALL PRIVILEGES ON huerta_usuarios.* TO 'huerta_app'@'localhost';
GRANT ALL PRIVILEGES ON huerta_cultivos.* TO 'huerta_app'@'localhost';
GRANT ALL PRIVILEGES ON huerta_tareas.* TO 'huerta_app'@'localhost';

-- 2. Catalogo de usuarios y cursos.
CREATE TABLE IF NOT EXISTS huerta_usuarios.usuarios (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  activo BIT(1) NOT NULL,
  contrasena_hash VARCHAR(255) NOT NULL,
  correo VARCHAR(255) NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  rol ENUM('ADMINISTRADOR','DOCENTE','ESTUDIANTE') NOT NULL,
  curso_id BIGINT NULL,
  UNIQUE KEY uk_usuarios_correo (correo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS huerta_usuarios.cursos (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(20) NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  UNIQUE KEY uk_cursos_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migraciones compatibles con versiones que carecen de curso_id.
SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_usuarios.usuarios ADD COLUMN curso_id BIGINT NULL',
 'SELECT 1')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_usuarios' AND TABLE_NAME='usuarios' AND COLUMN_NAME='curso_id');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

-- Docentes antiguos tenian el rol ADMINISTRADOR: ampliar ENUM antes de cambiarlos.
ALTER TABLE huerta_usuarios.usuarios
  MODIFY COLUMN rol ENUM('ADMINISTRADOR','DOCENTE','ESTUDIANTE') NOT NULL;

INSERT INTO huerta_usuarios.cursos (codigo,nombre) VALUES
('7-A','Septimo Basico A'),('7-B','Septimo Basico B'),('8-A','Octavo Basico A')
ON DUPLICATE KEY UPDATE codigo=VALUES(codigo);

-- Se agregan las cuentas ausentes. Si una ya existe, solo se corrige rol/curso.
-- Por ello no se reemplazan contrasenas personales ni nombres al reimportar.
INSERT INTO huerta_usuarios.usuarios
 (activo,contrasena_hash,correo,nombre,rol,curso_id)
SELECT b'1',d.clave,d.correo,d.nombre,d.rol,c.id FROM (
 SELECT 'profesor@huerta.cl' correo,'Profesor Juan' nombre,'Profesor123!' clave,'DOCENTE' rol,'7-A' codigo
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
WHERE TRUE
ON DUPLICATE KEY UPDATE rol=VALUES(rol),curso_id=VALUES(curso_id);

INSERT INTO huerta_usuarios.usuarios
 (activo,contrasena_hash,correo,nombre,rol,curso_id)
VALUES (b'1','AdminGlobal2026!','admin.global@huerta.cl',
        'Administradora General','ADMINISTRADOR',NULL)
ON DUPLICATE KEY UPDATE rol=VALUES(rol),curso_id=NULL;

-- 3. Sectores y cultivos. El curso de un sector existente NO se reasigna.
CREATE TABLE IF NOT EXISTS huerta_cultivos.sectores (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(255) NOT NULL,
  descripcion VARCHAR(1000) DEFAULT NULL,
  estado VARCHAR(255) NOT NULL,
  fecha_creacion VARCHAR(255) DEFAULT NULL,
  nombre VARCHAR(255) NOT NULL,
  responsable_id VARCHAR(255) DEFAULT NULL,
  ubicacion VARCHAR(255) DEFAULT NULL,
  curso_id BIGINT NULL,
  UNIQUE KEY uk_sectores_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS huerta_cultivos.cultivos (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  descripcion VARCHAR(1000) DEFAULT NULL,
  estado_desarrollo VARCHAR(255) DEFAULT NULL,
  fecha_cosecha_estimada VARCHAR(255) DEFAULT NULL,
  fecha_siembra VARCHAR(255) DEFAULT NULL,
  frecuencia_riego VARCHAR(255) DEFAULT NULL,
  nombre_comun VARCHAR(255) NOT NULL,
  sector_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_cultivos.sectores ADD COLUMN curso_id BIGINT NULL',
 'SELECT 1')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_cultivos' AND TABLE_NAME='sectores' AND COLUMN_NAME='curso_id');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

-- Datos de muestra, insertados solo cuando faltan. No se modifican sectores editados.
INSERT INTO huerta_cultivos.sectores
 (codigo,descripcion,estado,fecha_creacion,nombre,responsable_id,ubicacion,curso_id)
SELECT x.codigo,x.descripcion,'ACTIVO','2026-11-09',x.nombre,
       CAST(u.id AS CHAR),x.ubicacion,c.id
FROM (
 SELECT 'SEC-001' codigo,'Almacigos y hortalizas de hoja' descripcion,'Huerto Principal' nombre,'Patio norte' ubicacion,'profesor@huerta.cl' responsable,'7-A' curso
 UNION ALL SELECT 'SEC-002','Lechugas y hierbas','Huerto Secundario','Patio sur','profesor@huerta.cl','7-A'
 UNION ALL SELECT 'SEC-003','Hierbas aromaticas y medicinales','Jardin Aromatico','Patio este','maria.docente@huerta.cl','7-B'
 UNION ALL SELECT 'SEC-004','Hortalizas de raiz','Huerto de Raices','Patio oeste','pedro.docente@huerta.cl','8-A'
 UNION ALL SELECT 'SEC-005','Plantas de temporada','Invernadero Escolar','Invernadero','maria.docente@huerta.cl','7-B'
) x
JOIN huerta_usuarios.usuarios u ON u.correo=x.responsable
JOIN huerta_usuarios.cursos c ON c.codigo=x.curso
WHERE NOT EXISTS (SELECT 1 FROM huerta_cultivos.sectores s WHERE s.codigo=x.codigo);

-- Instancias antiguas: completar el curso solo cuando aun no se habia asignado.
UPDATE huerta_cultivos.sectores s JOIN huerta_usuarios.cursos c
 ON c.codigo=CASE
    WHEN s.codigo IN ('SEC-001','SEC-002') THEN '7-A'
    WHEN s.codigo IN ('SEC-003','SEC-005') THEN '7-B'
    WHEN s.codigo='SEC-004' THEN '8-A'
    ELSE NULL END
SET s.curso_id=c.id
WHERE s.curso_id IS NULL;

INSERT INTO huerta_cultivos.cultivos
 (descripcion,estado_desarrollo,fecha_cosecha_estimada,fecha_siembra,frecuencia_riego,nombre_comun,sector_id)
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
) x JOIN huerta_cultivos.sectores s ON s.codigo=x.codigo
WHERE NOT EXISTS (SELECT 1 FROM huerta_cultivos.cultivos c
                  WHERE c.sector_id=s.id AND c.nombre_comun=x.nombre);

-- 4. Tareas, reportes e historial. Mantener las mismas columnas que las entidades Spring.
CREATE TABLE IF NOT EXISTS huerta_tareas.tareas (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
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
  intento_actual INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS huerta_tareas.alertas (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  descripcion VARCHAR(2000) NOT NULL,
  estado VARCHAR(255) NOT NULL,
  fecha_reporte VARCHAR(255) DEFAULT NULL,
  sector_id BIGINT NOT NULL,
  tipo VARCHAR(255) NOT NULL,
  usuario_id BIGINT DEFAULT NULL,
  curso_id BIGINT DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Al importar sobre instalaciones anteriores, agregar solo las columnas ausentes.
SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_tareas.tareas ADD COLUMN comentario_estudiante VARCHAR(1000) NULL',
 'SELECT 1')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_tareas' AND TABLE_NAME='tareas' AND COLUMN_NAME='comentario_estudiante');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_tareas.tareas ADD COLUMN curso_id BIGINT NULL',
 'SELECT 1')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_tareas' AND TABLE_NAME='tareas' AND COLUMN_NAME='curso_id');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_tareas.alertas ADD COLUMN curso_id BIGINT NULL',
 'SELECT 1')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_tareas' AND TABLE_NAME='alertas' AND COLUMN_NAME='curso_id');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_tareas.tareas ADD COLUMN intento_actual INT NOT NULL DEFAULT 0',
 'SELECT 1')
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
  INDEX idx_historial_tarea_fecha (tarea_id,fecha,id),
  CONSTRAINT fk_historial_tarea FOREIGN KEY (tarea_id) REFERENCES huerta_tareas.tareas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Aislamiento de cursos en filas antiguas. Conservar fotos, comentarios e historial.
-- Se ocultan relaciones inconsistentes; una vez arregladas, el segundo UPDATE
-- vuelve a asignarles curso. No publicar datos de un curso al cambiar su sector.
UPDATE huerta_tareas.tareas t
LEFT JOIN huerta_usuarios.usuarios u ON u.id=t.estudiante_id
LEFT JOIN huerta_cultivos.sectores s ON s.id=t.sector_id
SET t.curso_id=NULL
WHERE t.curso_id IS NOT NULL
  AND (u.curso_id IS NULL OR s.curso_id IS NULL
       OR u.curso_id<>s.curso_id OR t.curso_id<>s.curso_id);

UPDATE huerta_tareas.tareas t
JOIN huerta_usuarios.usuarios u ON u.id=t.estudiante_id
JOIN huerta_cultivos.sectores s ON s.id=t.sector_id
SET t.curso_id=s.curso_id
WHERE t.curso_id IS NULL AND s.curso_id IS NOT NULL AND u.curso_id=s.curso_id;

UPDATE huerta_tareas.alertas a
LEFT JOIN huerta_usuarios.usuarios u ON u.id=a.usuario_id
LEFT JOIN huerta_cultivos.sectores s ON s.id=a.sector_id
SET a.curso_id=NULL
WHERE a.curso_id IS NOT NULL
  AND (s.curso_id IS NULL OR a.curso_id<>s.curso_id
       OR (a.usuario_id IS NOT NULL AND
           (u.curso_id IS NULL OR u.curso_id<>s.curso_id)));

UPDATE huerta_tareas.alertas a
JOIN huerta_cultivos.sectores s ON s.id=a.sector_id
LEFT JOIN huerta_usuarios.usuarios u ON u.id=a.usuario_id
SET a.curso_id=s.curso_id
WHERE a.curso_id IS NULL AND s.curso_id IS NOT NULL
  AND (a.usuario_id IS NULL OR u.curso_id=s.curso_id);

-- Tareas de muestra: insertar solo si alumno y sector pertenecen al mismo curso.
INSERT INTO huerta_tareas.tareas
 (descripcion,estado,estudiante_id,evidencia_url,comentario_estudiante,
  fecha_asignacion,fecha_vencimiento,observacion_docente,sector_id,titulo,curso_id)
SELECT x.descripcion,x.estado,u.id,NULL,NULL,'2026-11-09',x.vencimiento,NULL,
       s.id,x.titulo,s.curso_id
FROM (
 SELECT 'SEC-001' codigo,'estudiante@huerta.cl' correo,'Riego de tomates' titulo,'Regar los almacigos de tomates y revisar la humedad' descripcion,'PENDIENTE' estado,'2026-11-10' vencimiento
 UNION ALL SELECT 'SEC-002','estudiante@huerta.cl','Revision de lechugas','Revisar el desarrollo de las lechugas','PENDIENTE','2026-11-12'
 UNION ALL SELECT 'SEC-003','camila@huerta.cl','Riego de albahaca','Regar albahaca y revisar hojas','PENDIENTE','2026-11-18'
 UNION ALL SELECT 'SEC-003','matias@huerta.cl','Control de menta','Revisar humedad y crecimiento de menta','EN_PROGRESO','2026-11-19'
 UNION ALL SELECT 'SEC-004','diego@huerta.cl','Siembra de zanahorias','Preparar surcos y sembrar zanahorias','PENDIENTE','2026-11-20'
 UNION ALL SELECT 'SEC-004','diego@huerta.cl','Revision de rabanos','Controlar germinacion de rabanos','PENDIENTE_VALIDACION','2026-11-16'
 UNION ALL SELECT 'SEC-005','sofia@huerta.cl','Ventilacion del invernadero','Comprobar ventilacion y temperatura','EN_PROGRESO','2026-11-17'
 UNION ALL SELECT 'SEC-005','camila@huerta.cl','Riego de pepinos','Regar pepinos y registrar observaciones','PENDIENTE','2026-11-19'
 UNION ALL SELECT 'SEC-001','valentina@huerta.cl','Control de plagas tomates','Inspeccionar hojas de tomate en busca de plagas','PENDIENTE','2026-11-21'
 UNION ALL SELECT 'SEC-002','valentina@huerta.cl','Cosecha de hojas','Revisar hojas listas para cosecha','APROBADA','2026-11-15'
) x
JOIN huerta_usuarios.usuarios u ON u.correo=x.correo
JOIN huerta_cultivos.sectores s ON s.codigo=x.codigo AND s.curso_id=u.curso_id
WHERE s.curso_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM huerta_tareas.tareas t
                  WHERE t.titulo=x.titulo AND t.sector_id=s.id);

INSERT INTO huerta_tareas.tareas
 (descripcion,estado,estudiante_id,evidencia_url,comentario_estudiante,
  fecha_asignacion,fecha_vencimiento,observacion_docente,sector_id,titulo,curso_id)
SELECT demo.descripcion,
       IF(demo.justificacion IS NULL,'PENDIENTE','PENDIENTE_VALIDACION'),
       u.id,NULL,demo.justificacion,'2026-11-16','2026-11-30',
       NULL,s.id,demo.titulo,c.id
FROM (
 SELECT '7-A' curso,'SEC-001' codigo,'estudiante@huerta.cl' correo,
        'Informe de germinacion 7-A' titulo,'Registrar el avance del semillero del 7-A' descripcion,
        'Regue las plantas y registre la humedad del 7-A para la revision docente.' justificacion
 UNION ALL SELECT '7-A','SEC-002','valentina@huerta.cl','Riego de lechugas 7-A','Regar lechugas del curso 7-A',NULL
 UNION ALL SELECT '7-B','SEC-003','camila@huerta.cl','Informe de germinacion 7-B','Registrar el avance del semillero del 7-B',
        'Revise el crecimiento de las semillas del 7-B y anote los cambios del dia.'
 UNION ALL SELECT '7-B','SEC-005','sofia@huerta.cl','Riego del invernadero 7-B','Comprobar plantas del invernadero 7-B',NULL
 UNION ALL SELECT '8-A','SEC-004','diego@huerta.cl','Registro de zanahorias 8-A','Medir crecimiento del cultivo del 8-A',NULL
) demo
JOIN huerta_usuarios.cursos c ON c.codigo=demo.curso
JOIN huerta_usuarios.usuarios u ON u.correo=demo.correo AND u.curso_id=c.id
JOIN huerta_cultivos.sectores s ON s.codigo=demo.codigo AND s.curso_id=c.id
WHERE NOT EXISTS (SELECT 1 FROM huerta_tareas.tareas t
                  WHERE t.titulo=demo.titulo AND t.sector_id=s.id);

-- Reportes de muestra solo si quien reporta comparte el curso del sector.
INSERT INTO huerta_tareas.alertas
 (descripcion,estado,fecha_reporte,sector_id,tipo,usuario_id,curso_id)
SELECT x.descripcion,'PENDIENTE','2026-11-09',s.id,x.tipo,u.id,s.curso_id
FROM (
 SELECT 'SEC-001' codigo,'estudiante@huerta.cl' correo,'Revisar humedad del sector principal' descripcion,'RIEGO' tipo
 UNION ALL SELECT 'SEC-002','estudiante@huerta.cl','Inspeccionar hojas de lechuga','REVISION'
 UNION ALL SELECT 'SEC-003','camila@huerta.cl','Controlar riego de hierbas aromaticas','RIEGO'
 UNION ALL SELECT 'SEC-004','diego@huerta.cl','Revisar germinacion de zanahorias','REVISION'
 UNION ALL SELECT 'SEC-005','sofia@huerta.cl','Verificar temperatura del invernadero','TEMPERATURA'
) x
JOIN huerta_cultivos.sectores s ON s.codigo=x.codigo
JOIN huerta_usuarios.usuarios u ON u.correo=x.correo AND u.curso_id=s.curso_id
WHERE s.curso_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM huerta_tareas.alertas a
                  WHERE a.sector_id=s.id AND a.descripcion=x.descripcion);

-- 5. Comprobaciones. Ninguna muestra contrasenas ni altera datos.
SELECT 'cursos' tabla,COUNT(*) registros FROM huerta_usuarios.cursos
UNION ALL SELECT 'usuarios',COUNT(*) FROM huerta_usuarios.usuarios
UNION ALL SELECT 'sectores',COUNT(*) FROM huerta_cultivos.sectores
UNION ALL SELECT 'cultivos',COUNT(*) FROM huerta_cultivos.cultivos
UNION ALL SELECT 'tareas',COUNT(*) FROM huerta_tareas.tareas
UNION ALL SELECT 'alertas',COUNT(*) FROM huerta_tareas.alertas
UNION ALL SELECT 'historial',COUNT(*) FROM huerta_tareas.tarea_historial;

SELECT u.correo,u.rol,COALESCE(c.codigo,'TODOS') AS curso,
       IF(u.activo=b'1','ACTIVA','INACTIVA') AS estado
FROM huerta_usuarios.usuarios u
LEFT JOIN huerta_usuarios.cursos c ON c.id=u.curso_id
WHERE u.correo LIKE '%@huerta.cl'
ORDER BY u.rol,u.correo;

-- Deben devolver 0 filas. Si hay datos antiguos incompatibles, corregirlos
-- manualmente SIN mover registros a otro curso ni cambiar evidencias guardadas.
SELECT t.id,t.titulo,t.estudiante_id,t.sector_id
FROM huerta_tareas.tareas t WHERE t.curso_id IS NULL;
SELECT a.id,a.descripcion,a.usuario_id,a.sector_id
FROM huerta_tareas.alertas a WHERE a.curso_id IS NULL;
SELECT s.id,s.codigo,s.nombre
FROM huerta_cultivos.sectores s WHERE s.curso_id IS NULL;
