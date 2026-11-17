-- HUERTA ESCOLAR | MIGRACION SEGURA PARA ESQUEMA YA EXISTENTE
-- Ejecutar solo si tareas-service falla por tabla tarea_historial o intento_actual.
-- No borra registros; puede reejecutarse. Compatible con MySQL 8 y MariaDB 10.4.
-- Nota: las entregas previas sobrescritas NO pueden reconstruirse.
USE huerta_tareas;
SET @ddl = (SELECT IF(COUNT(*)=0,
 'ALTER TABLE huerta_tareas.tareas ADD COLUMN intento_actual INT NOT NULL DEFAULT 0',
 'DO 0')
 FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='huerta_tareas' AND TABLE_NAME='tareas' AND COLUMN_NAME='intento_actual');
PREPARE p FROM @ddl;
EXECUTE p;
DEALLOCATE PREPARE p;
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
SELECT 'ok' AS estado, COUNT(*) AS registros_historial FROM huerta_tareas.tarea_historial;
