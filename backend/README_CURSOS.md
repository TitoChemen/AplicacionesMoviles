# Huerta Escolar — roles y evidencias

Ver el instructivo completo en `../README_CAMBIOS_2026_11_16.md`.

- **DOCENTE:** solo tareas, alumnos y sectores de su curso.
- **ADMINISTRADOR:** cuenta global sin curso, puede ver y validar entregas de todos los cursos.
- **ESTUDIANTE:** solo sus propias tareas.
- La justificación del estudiante se guarda en `tareas.comentario_estudiante` junto con la fotografía.

Importar `sql/init_huerta_escolar_cursos.sql` antes de arrancar los servicios. Luego ejecutar `bash scripts/probar_roles_ubuntu.sh` desde `backend` o `bash backend/scripts/probar_roles_ubuntu.sh` desde la raíz.
