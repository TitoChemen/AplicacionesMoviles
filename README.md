# Huerta Escolar · DSY1105

Aplicación Android en Kotlin y Jetpack Compose para organizar sectores, cultivos, tareas, evidencias fotográficas y reportes de un huerto escolar. El problema abordado es el seguimiento de actividades de distintos cursos sin mezclar sus datos. Esta entrega es un avance de la Evaluación Parcial 4 y del Examen Final Transversal.

## Equipo

En el código recibido figuran Kevin, Oscar y Jaime. **La pauta de la Evaluación Parcial 4 **: La autoría real y los commits deben quedar en GitHub; no se sustituyen con este documento.

## Arquitectura y permisos

`Frontend/` contiene la app Android. `backend/` contiene cuatro proyectos Spring Boot: `api-gateway` (8080), `usuarios-service` (8081), `huerta-service` (8082) y `tareas-service` (8083). MariaDB mantiene tres bases: `huerta_usuarios`, `huerta_cultivos` y `huerta_tareas`. Retrofit usa el gateway para todos los datos del huerto. Un cliente Retrofit separado consulta el clima exterior en Open-Meteo.

| Función | Administrador global | Docente | Estudiante |
| --- | --- | --- | --- |
| Ver sectores/cultivos | Todos los cursos | Solo su curso | Solo su curso |
| Crear, editar, archivar, eliminar o cambiar curso del sector | Sí | No | No |
| Crear cultivos | Todos los cursos | Solo su curso | No |
| Asignar y validar tareas | Todos los cursos | Solo su curso | No |
| Ver tareas | Todas | Solo su curso | Solo sus tareas |
| Reportar problema | Revisa reportes | Revisa reportes | Sectores activos de su curso |
| Revisar estado de reportes | Todos | Solo su curso | Solo sus reportes |

El servidor usa el `cursoId` y el rol del JWT validado. No se fía del curso enviado por el cliente para autorizar lecturas. La asignación de sectores exige un curso existente. Al cambiar el curso, el sector y sus cultivos asociados por `sectorId` pasan al nuevo curso; si tiene tareas o alertas, la API responde **409 Conflict** y conserva el curso anterior. El cambio depende de que `tareas-service` y el gateway estén disponibles para comprobar esas dependencias. Las cuentas de docente y alumno de un curso no pueden consultar los sectores de otro, ni siquiera usando el ID directamente.

## Endpoints principales

| Servicio | Endpoints a través del gateway | Uso en Android |
| --- | --- | --- |
| Usuarios | `POST /api/auth/login`, `GET /api/usuarios`, `GET /api/cursos` | Sesión, estudiantes y cursos para administrador |
| Huerta | `GET/POST /api/sectores`, `GET/PUT/DELETE /api/sectores/{id}` | Lista, alta, edición con curso, archivo y baja |
| Huerta | `GET/POST /api/cultivos`, `GET/PUT/DELETE /api/cultivos/{id}` | CRUD de cultivos |
| Tareas | `GET/POST /api/tareas`, `PUT/DELETE /api/tareas/{id}`, `PATCH /api/tareas/{id}/validacion`, `GET /api/tareas/{id}/historial`, `POST /api/tareas/{id}/evidencias/archivo` | CRUD de tareas, validación, historial y fotografía |
| Alertas | `GET/POST /api/alertas`, `PATCH /api/alertas/{id}/estado` | Reportar problemas y revisarlos |
| Externa | `GET https://api.open-meteo.com/v1/forecast?latitude=-33.4489&longitude=-70.6693&current=temperature_2m,relative_humidity_2m,precipitation&timezone=America/Santiago` | Temperatura, humedad del **aire** y lluvia de referencia en Santiago |

El dato de Open-Meteo se presenta separado de los sensores **simulados** de temperatura y humedad de suelo. Si falla el servicio externo, los sectores y cultivos siguen funcionando. Documentación de la API: https://open-meteo.com/en/docs

## Levantar el proyecto

1. Instalar Java 21, MariaDB (por ejemplo XAMPP), Android Studio con Android SDK 34 y conexión a internet para resolver dependencias.
2. Importar `backend/sql/init_huerta_escolar_cursos.sql` con un usuario administrador de MariaDB. En una instalación existente, respaldar primero las bases: el script actualiza algunas cuentas y sectores de demostración. Las claves de ejemplo y `PLAINTEXT_DEV=true` son **solo para la demo local**; ver `backend/sql/Cuentas.txt`.
3. Configurar **las mismas** variables de entorno en los servicios: `DB_PASSWORD` con la clave `huerta_app` del SQL y `JWT_SECRET` como Base64 de al menos 32 bytes, por ejemplo `openssl rand -base64 48`. Para las cuentas demo en texto plano, configurar `PLAINTEXT_DEV=true` en `usuarios-service`.
4. Desde cada carpeta de `backend/`, ejecutar `./mvnw spring-boot:run`, en orden: `usuarios-service`, `huerta-service`, `tareas-service` y `api-gateway`. Verificar que responda `http://localhost:8080/api/auth/login` mediante `POST`.
5. Abrir `Frontend/` como proyecto de Android Studio. En el emulador, la app usa `http://10.0.2.2:8080/`. Para un teléfono real, compilar con la IP local del PC y la misma red Wi-Fi, por ejemplo `./gradlew :app:assembleDebug -PHUERTA_API_BASE_URL=http://192.168.1.10:8080/`. Ajustar firewall si corresponde. La app debe poder alcanzar el gateway desde el dispositivo.
6. Iniciar sesión con las cuentas de `backend/sql/Cuentas.txt`. El administrador crea sectores en 7-A, 7-B u 8-A; un estudiante solo ve sectores de su curso. Para demostrar el traslado, crear un sector **sin tareas ni alertas**, entrar a editar y seleccionar otro curso.

## Pruebas y validación

- Android: `cd Frontend && ./gradlew :app:createDebugUnitTestCoverageReport`. El informe queda en `app/build/reports/coverage/test/debug/index.html` (pruebas del mapeo, límites y fallos de la API externa).
- Backend: `cd backend/huerta-service && ./mvnw test` (pruebas de asignación, cambio de curso y bloqueo de acceso docente). Ejecutar `./mvnw test` en los otros servicios.
- Integración con los cuatro procesos y la base activos: `bash backend/scripts/probar_roles_ubuntu.sh` y `bash backend/scripts/probar_sectores_cursos_ubuntu.sh`. El segundo crea y elimina un sector temporal, prueba accesos cruzados, y confirma el rechazo 409 si hay tareas.
- La pauta exige **al menos 80% de la lógica** de Android cubierta con pruebas. Las pruebas agregadas cubren el repositorio del clima y reglas críticas de sectores en el backend, pero **todavía no existe medición que demuestre el 80% de toda la lógica Android**. Añadir pruebas de AuthViewModel, SectorViewModel, TareaViewModel y AlertaViewModel con dependencias inyectables, ejecutar el reporte y adjuntar el resultado antes de marcar este criterio como cumplido.

## APK release firmado

`Frontend/signing/huerta-demo-release.jks` es una **llave académica de demostración** generada para esta entrega. `Frontend/signing/demo.properties` contiene sus credenciales y `Frontend/app/build.gradle.kts` configura el bloque `release`. La llave compartida no debe usarse para publicar una aplicación real.

```bash
cd Frontend
./gradlew :app:assembleRelease -PHUERTA_API_BASE_URL=http://IP_DEL_PC:8080/
# Resultado: app/build/outputs/apk/release/app-release.apk
"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

La generación y verificación del APK requieren Android SDK y dependencias Gradle; **el paquete entregado no contiene un APK compilado**. Para la entrega en GitHub y AVA, generar el APK desde el equipo del curso, conservar el `.jks`, verificar la firma, agregar el archivo al repositorio de entrega (la regla general `*.apk` de `.gitignore` exige `git add -f`) y capturar la evidencia requerida por la pauta. Usar la URL del gateway que sea accesible desde el teléfono en la defensa.

## Checklist

Consultar [CHECKLIST_ENTREGA.md](CHECKLIST_ENTREGA.md)
