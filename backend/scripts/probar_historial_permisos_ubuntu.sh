#!/usr/bin/env bash
# Consulta de permisos SIN modificar datos. Requiere SQL demo, cuatro servicios corriendo, curl, jq.
set -euo pipefail
BASE_URL="${API_URL:-http://localhost:8080}"
for dep in curl jq; do command -v "$dep" >/dev/null || { echo "Instala: sudo apt install curl jq" >&2; exit 1; }; done
TMP="$(mktemp)"; trap 'rm -f "$TMP"' EXIT
login() {
  local json
  json="$(jq -cn --arg correo "$1" --arg contrasena "$2" '{correo:$correo,contrasena:$contrasena}')"
  curl -fsS --max-time 15 -H 'Content-Type: application/json' -d "$json" \
    "${BASE_URL}/api/auth/login" | jq -er '.accessToken // .token // empty'
}
get() { curl -fsS --max-time 15 -H "Authorization: Bearer $1" "${BASE_URL}$2"; }
code() { curl -sS --max-time 15 -o "$TMP" -w '%{http_code}' -H "Authorization: Bearer $1" "${BASE_URL}$2"; }
A="$(login 'profesor@huerta.cl' 'Profesor123!')"
B="$(login 'profesor.7b@huerta.cl' 'Profesor7B2026!')"
G="$(login 'admin.global@huerta.cl' 'AdminGlobal2026!')"
E="$(login 'estudiante@huerta.cl' 'Estudiante123!')"
COURSES="$(get "$G" '/api/cursos')"
[[ "$(jq 'length' <<<"$COURSES")" -ge 2 ]] || { echo 'FALLO: admin sin cursos'; exit 1; }
[[ "$(jq -r '[.[].codigo] | index("7-A") != null and index("7-B") != null' <<<"$COURSES")" == true ]] || { echo 'FALLO: faltan 7-A/7-B'; exit 1; }
echo 'OK: admin ve cursos 7-A y 7-B para seleccionar al crear sectores'
[[ "$(code "$A" '/api/cursos')" == '403' ]] || { echo 'FALLO: docente no debe consultar cursos globales'; exit 1; }
[[ "$(code "$E" '/api/cursos')" == '403' ]] || { echo 'FALLO: estudiante no debe consultar cursos globales'; exit 1; }
echo 'OK: docentes y estudiantes no tienen acceso al directorio global de cursos'
TA="$(get "$A" '/api/tareas')"; TB="$(get "$B" '/api/tareas')"
ID_A="$(jq -er '.[0].id' <<<"$TA")"; ID_B="$(jq -er '.[0].id' <<<"$TB")"
for case in "A:$ID_B" "B:$ID_A"; do
  role="${case%%:*}"; id="${case##*:}"
  token="$A"; [[ "$role" == 'B' ]] && token="$B"
  status="$(code "$token" "/api/tareas/$id/historial")"
  [[ "$status" == 403 || "$status" == 404 ]] || { echo "FALLO: $role pudo ver historial ajeno HTTP $status"; exit 1; }
done
echo 'OK: docente 7-A no lee historial de 7-B y viceversa'
[[ "$(code "$G" "/api/tareas/$ID_A/historial")" == 200 ]] || { echo 'FALLO: admin sin historial 7-A'; exit 1; }
[[ "$(code "$G" "/api/tareas/$ID_B/historial")" == 200 ]] || { echo 'FALLO: admin sin historial 7-B'; exit 1; }
[[ "$(code "$B" "/api/tareas/$ID_B/historial")" == 200 ]] || { echo 'FALLO: docente 7-B sin historial propio'; exit 1; }
echo 'OK: admin ve ambos historiales; docente ve el de su curso'
echo 'Pruebas de lectura completadas. Para comprobar creación, subir evidencias y rechazo, usa la app.'
