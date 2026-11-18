# Revisión de Evaluación Parcial 4 · DSY1105

Estado revisado contra la pauta adjunta por el equipo. **Implementado** significa presente en código; **pendiente de demostrar** requiere ejecutar o aportar evidencia real.

| Indicador | Estado | Evidencia y acción pendiente |
| --- | --- | --- |
| IE 3.1.1 · Pantallas, formularios y navegación (15%) | Implementado en código, pendiente de defensa | Login, sectores, cultivos, tareas, evidencias, historial, reporte y revisión de alertas. Probar en emulador/teléfono con los cuatro servicios encendidos; no se pudo ejecutar aquí. |
| IE 3.1.2 · Microservicios, BD y endpoints (15%) | Presente en código, pendiente de ejecutar | Gateway, usuarios, huerta y tareas; SQL canónico en `backend/sql/init_huerta_escolar_cursos.sql`. Confirmar arranque contra MariaDB y llamadas reales. |
| IE 3.1.3 · Integración y CRUD (15%) | Presente en código, pendiente de prueba real | Retrofit conectado con sectores, cultivos, tareas y alertas. Sectores: CRUD solo admin. Cultivos y tareas: CRUD para personal autorizado. Reportes: alta/lectura/cambio de estado (sin edición de texto ni borrado en UI). Probar todos los caminos de la defensa. |
| IE 3.1.4 · API externa Retrofit (15%) | Implementado en código, pendiente de prueba con red | Open-Meteo para clima exterior de Santiago, tarjeta visual separada y error recuperable. |
| IE 3.2.1 · Pruebas unitarias ≥80% lógica Android (15%) | **Pendiente** | Hay pruebas nuevas de ClimaRepository y pruebas del backend para sectores. Falta ejecutar, medir cobertura Android y cubrir ViewModels/Repository hasta 80%. |
| IE 3.3.1 · APK release firmado y `.jks` (10%) | Configuración y `.jks` presentes; **APK pendiente** | Compilar `:app:assembleRelease` en Android Studio con SDK, verificar con `apksigner`, agregar APK y captura de firma a entrega. |
| IE 3.3.2 · GitHub y Trello, colaboración real (15%) | **Pendiente de evidencia** | En el ZIP hay historial Git; verificar commits de cada miembro en repositorio público y adjuntar tablero Trello con tareas distribuidas. |
| Contexto EFT y documentación técnica | Documento presente | `README.md` describe contexto, roles, arquitectura, endpoints, instalación, pruebas y firma; confirmar alcance acordado con el docente. |
| Forma de trabajo | **Revisar** | En el código se mencionan Kevin, Oscar y Jaime, pero esta evaluación exige parejas. Determinar pareja que entrega. |
| AVA y fecha | **Pendiente de equipo** | Una persona sube la entrega grupal antes del plazo y cada integrante activa el enlace individual según la pauta. No registrar cambios luego de la fecha límite. |

## Escenarios mínimos para defensa

1. Iniciar sesión como admin, docente 7-A, docente 7-B y estudiante 7-A.
2. Admin crea un sector 7-A; 7-B no lo ve ni por URL/ID. Docente y estudiante 7-A sí lo ven.
3. Docente intenta `POST` y `PUT` de sectores: **403**. Admin cambia a 7-B; 7-A deja de verlo y 7-B lo ve. Con tareas/alertas existentes el cambio debe responder **409**.
4. Estudiante 7-A envía evidencia y justificación de su tarea; docente 7-A la valida, docente 7-B no puede leerla.
5. Estudiante crea reporte de un sector de su curso; docente lo ve y marca resuelto.
6. Mostrar clima exterior de Open-Meteo y sensores simulados diferenciados; si falla el clima, la navegación y los microservicios continúan operando.
7. Ejecutar pruebas Android con informe de cobertura ≥80%, enseñar APK **real** verificado y captura de firma, además del historial de commits y Trello.
