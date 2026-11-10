-- HUERTA ESCOLAR | init_huerta_escolar_v3.sql
-- Inicializacion de desarrollo para MySQL 8 / MariaDB 10.5+
-- Crea bases y tablas si no existen; NO borra ni migra tablas existentes.
-- Requiere privilegios CREATE DATABASE, CREATE TABLE, SELECT e INSERT.
-- Credenciales y datos de ejemplo: SOLO DESARROLLO.
-- El usuario de conexion de los microservicios se configura por separado.
-- NOTA: verificar los hashes BCrypt con el PasswordEncoder del backend antes
-- de utilizar este script en una instalacion nueva.
-- NOTA: comentario_estudiante presupone que la entidad Tarea y el servicio
-- de evidencias ya se actualizaron para persistir ese campo.

CREATE DATABASE IF NOT EXISTS huerta_usuarios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS huerta_cultivos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS huerta_tareas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE huerta_usuarios;
CREATE TABLE IF NOT EXISTS usuarios (
  id BIGINT NOT NULL AUTO_INCREMENT,
  activo BIT(1) NOT NULL,
  contrasena_hash VARCHAR(255) NOT NULL,
  correo VARCHAR(255) NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  rol ENUM('ADMINISTRADOR','ESTUDIANTE') NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_usuarios_correo (correo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Credenciales esperadas (verificar correspondencia de hashes):
-- profesor@huerta.cl / Profesor123!
-- estudiante@huerta.cl / Estudiante123!
INSERT INTO usuarios (activo, contrasena_hash, correo, nombre, rol)
VALUES (b'1', '$2y$10$9osUbP8Jl0V4Wv9J7OJESeEq0CvortLPcOBNSmnIaplT0HfMrs11u', 'profesor@huerta.cl', 'Profesor Juan', 'ADMINISTRADOR')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO usuarios (activo, contrasena_hash, correo, nombre, rol)
VALUES (b'1', '$2y$10$Fv5xAIJXG7ks/3gE7EVs1O1nagaZbYol.kaAxFBDEhq0Kugk2liCO', 'estudiante@huerta.cl', 'Kevin Estudiante', 'ESTUDIANTE')
ON DUPLICATE KEY UPDATE id = id;

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

INSERT INTO sectores (codigo, descripcion, estado, fecha_creacion, nombre, responsable_id, ubicacion)
SELECT 'SEC-001', 'Almacigos y hortalizas de hoja', 'ACTIVO', '2026-11-09', 'Huerto Principal', CAST(u.id AS CHAR), 'Patio norte'
FROM huerta_usuarios.usuarios u
WHERE u.correo = 'profesor@huerta.cl'
  AND NOT EXISTS (SELECT 1 FROM sectores s WHERE s.codigo = 'SEC-001');

INSERT INTO sectores (codigo, descripcion, estado, fecha_creacion, nombre, responsable_id, ubicacion)
SELECT 'SEC-002', 'Lechugas y hierbas', 'ACTIVO', '2026-11-09', 'Huerto Secundario', CAST(u.id AS CHAR), 'Patio sur'
FROM huerta_usuarios.usuarios u
WHERE u.correo = 'profesor@huerta.cl'
  AND NOT EXISTS (SELECT 1 FROM sectores s WHERE s.codigo = 'SEC-002');

INSERT INTO cultivos (descripcion, estado_desarrollo, fecha_cosecha_estimada, fecha_siembra, frecuencia_riego, nombre_comun, sector_id)
SELECT 'Cultivo de tomates cherry', 'CRECIMIENTO', '2027-02-15', '2026-11-09', 'Diario', 'Tomate Cherry', s.id
FROM sectores s
WHERE s.codigo = 'SEC-001'
  AND NOT EXISTS (SELECT 1 FROM cultivos c WHERE c.sector_id = s.id AND c.nombre_comun = 'Tomate Cherry');

INSERT INTO cultivos (descripcion, estado_desarrollo, fecha_cosecha_estimada, fecha_siembra, frecuencia_riego, nombre_comun, sector_id)
SELECT 'Cultivo de lechugas', 'CRECIMIENTO', '2027-01-10', '2026-11-09', 'Cada 2 dias', 'Lechuga', s.id
FROM sectores s
WHERE s.codigo = 'SEC-002'
  AND NOT EXISTS (SELECT 1 FROM cultivos c WHERE c.sector_id = s.id AND c.nombre_comun = 'Lechuga');

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
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tareas (descripcion, estado, estudiante_id, evidencia_url, comentario_estudiante, fecha_asignacion, fecha_vencimiento, observacion_docente, sector_id, titulo)
SELECT 'Regar los almácigos de tomates y revisar la humedad', 'PENDIENTE', u.id, NULL, NULL, '2026-11-09', '2026-11-10', NULL, s.id, 'Riego de tomates'
FROM huerta_usuarios.usuarios u
JOIN huerta_cultivos.sectores s ON s.codigo = 'SEC-001'
WHERE u.correo = 'estudiante@huerta.cl'
  AND NOT EXISTS (SELECT 1 FROM tareas t WHERE t.titulo = 'Riego de tomates' AND t.sector_id = s.id);

INSERT INTO tareas (descripcion, estado, estudiante_id, evidencia_url, comentario_estudiante, fecha_asignacion, fecha_vencimiento, observacion_docente, sector_id, titulo)
SELECT 'Revisar el desarrollo de las lechugas', 'PENDIENTE', u.id, NULL, NULL, '2026-11-09', '2026-11-12', NULL, s.id, 'Revisión de lechugas'
FROM huerta_usuarios.usuarios u
JOIN huerta_cultivos.sectores s ON s.codigo = 'SEC-002'
WHERE u.correo = 'estudiante@huerta.cl'
  AND NOT EXISTS (SELECT 1 FROM tareas t WHERE t.titulo = 'Revisión de lechugas' AND t.sector_id = s.id);

INSERT INTO alertas (descripcion, estado, fecha_reporte, sector_id, tipo, usuario_id)
SELECT 'Revisar humedad del sector principal', 'PENDIENTE', '2026-11-09', s.id, 'RIEGO', u.id
FROM huerta_cultivos.sectores s
JOIN huerta_usuarios.usuarios u ON u.correo = 'estudiante@huerta.cl'
WHERE s.codigo = 'SEC-001'
  AND NOT EXISTS (SELECT 1 FROM alertas a WHERE a.sector_id = s.id AND a.descripcion = 'Revisar humedad del sector principal');

-- Resumen de registros al finalizar
SELECT 'usuarios' AS tabla, COUNT(*) AS registros FROM huerta_usuarios.usuarios
UNION ALL SELECT 'sectores', COUNT(*) FROM huerta_cultivos.sectores
UNION ALL SELECT 'cultivos', COUNT(*) FROM huerta_cultivos.cultivos
UNION ALL SELECT 'tareas', COUNT(*) FROM huerta_tareas.tareas
UNION ALL SELECT 'alertas', COUNT(*) FROM huerta_tareas.alertas;
