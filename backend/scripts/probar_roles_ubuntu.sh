#!/usr/bin/env bash
# Huerta Escolar — prueba de permisos SIN modificar datos
# Ejecutar desde Ubuntu: bash backend/scripts/probar_roles_ubuntu.sh
set -euo pipefail
BASE_URL="${API_URL:-http://localhost:8080}"
for dep in curl jq; do command -v "$dep" >/dev/null || { echo "Falta $dep. Instala: sudo apt install curl jq" >&2; exit 1; }; done
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT
PASS=0

ok() { echo "✅ $1"; PASS=$((PASS+1)); }
fail() { echo "❌ $1" >&2; exit 1; }
login() {
  local email="$1" password="$2" response token
  response="$(curl --connect-timeout 5 --max-time 15 -fsS -X POST "${BASE_URL}/api/auth/login" \
    -H 'Content-Type: application/json' \
    --data "$(jq -nc --arg correo "$email" --arg contrasena "$password" '{correo:$correo,contrasena:$contrasena}')")"
  token="$(jq -er '.accessToken // .token // empty' <<<"$response")" || fail "No hubo JWT para $email"
  printf '%s' "$token"
}
get() { curl --connect-timeout 5 --max-time 15 -fsS -H "Authorization: Bearer $1" "${BASE_URL}$2"; }
code() { curl --connect-timeout 5 --max-time 15 -sS -o "$TMP" -w '%{http_code}' -H "Authorization: Bearer $1" "${BASE_URL}$2"; }
check_denied() {
  local status
  status="$(code "$1" "$2")"
  [[ "$status" == '403' || "$status" == '404' ]] && ok "$3 (HTTP $status)" || fail "$3: HTTP $status (esperado 403/404)"
}

echo "Pruebas de roles contra ${BASE_URL}"
A="$(login 'profesor@huerta.cl' 'Profesor123!')"
B="$(login 'profesor.7b@huerta.cl' 'Profesor7B2026!')"
G="$(login 'admin.global@huerta.cl' 'AdminGlobal2026!')"
E="$(login 'estudiante@huerta.cl' 'Estudiante123!')"

PROFILE_A="$(get "$A" '/api/usuarios/me')"
PROFILE_B="$(get "$B" '/api/usuarios/me')"
PROFILE_G="$(get "$G" '/api/usuarios/me')"
PROFILE_E="$(get "$E" '/api/usuarios/me')"
CA="$(jq -er '.cursoId' <<<"$PROFILE_A")"
CB="$(jq -er '.cursoId' <<<"$PROFILE_B")"
[[ "$CA" != "$CB" ]] || fail 'Ambos docentes tienen el mismo curso'
[[ "$(jq -r '.rol' <<<"$PROFILE_A")" == 'DOCENTE' ]] || fail '7-A sin rol DOCENTE'
[[ "$(jq -r '.rol' <<<"$PROFILE_B")" == 'DOCENTE' ]] || fail '7-B sin rol DOCENTE'
[[ "$(jq -r '.rol' <<<"$PROFILE_G")" == 'ADMINISTRADOR' ]] || fail 'Admin sin rol global'
[[ "$(jq -r '.cursoId // ""' <<<"$PROFILE_G")" == '' ]] || fail 'Admin global ligado a un curso'
ok 'Docentes 7-A/7-B y administrador global correctamente autenticados'

TA="$(get "$A" '/api/tareas')"
TB="$(get "$B" '/api/tareas')"
TG="$(get "$G" '/api/tareas')"
TE="$(get "$E" '/api/tareas')"
[[ "$(jq 'length' <<<"$TA")" -gt 0 ]] || fail '7-A sin tareas (revisa SQL)'
[[ "$(jq 'length' <<<"$TB")" -gt 0 ]] || fail '7-B sin tareas (revisa SQL)'
[[ "$(jq --argjson c "$CA" 'all(.[]; .cursoId == $c)' <<<"$TA")" == true ]] || fail '7-A recibió tareas de otros cursos'
[[ "$(jq --argjson c "$CB" 'all(.[]; .cursoId == $c)' <<<"$TB")" == true ]] || fail '7-B recibió tareas de otros cursos'
ok '7-A y 7-B solo listan tareas de su propio curso'
TAREA_A="$(jq -er '.[0].id' <<<"$TA")"
TAREA_B="$(jq -er '.[0].id' <<<"$TB")"
check_denied "$A" "/api/tareas/$TAREA_B" '7-A NO puede leer tarea de 7-B por ID'
check_denied "$B" "/api/tareas/$TAREA_A" '7-B NO puede leer tarea de 7-A por ID'
[[ "$(code "$G" "/api/tareas/$TAREA_A")" == '200' ]] || fail 'Admin no puede consultar 7-A'
[[ "$(code "$G" "/api/tareas/$TAREA_B")" == '200' ]] || fail 'Admin no puede consultar 7-B'
[[ "$(jq --argjson a "$CA" --argjson b "$CB" 'any(.[]; .cursoId == $a) and any(.[]; .cursoId == $b)' <<<"$TG")" == true ]] || fail 'Admin no lista tareas de los dos cursos'
ok 'Administrador global puede consultar tareas de ambos cursos'
SID="$(jq -er '.id' <<<"$PROFILE_E")"
[[ "$(jq --argjson id "$SID" 'all(.[]; .estudianteId == $id)' <<<"$TE")" == true ]] || fail 'Estudiante ve tareas ajenas'
ok 'Estudiante solo recibe sus tareas'

UA="$(get "$A" '/api/usuarios')"
UB="$(get "$B" '/api/usuarios')"
UG="$(get "$G" '/api/usuarios')"
[[ "$(jq --argjson c "$CA" 'all(.[]; .cursoId == $c)' <<<"$UA")" == true ]] || fail '7-A ve usuarios ajenos'
[[ "$(jq --argjson c "$CB" 'all(.[]; .cursoId == $c)' <<<"$UB")" == true ]] || fail '7-B ve usuarios ajenos'
[[ "$(jq --argjson a "$CA" --argjson b "$CB" 'any(.[]; .cursoId == $a) and any(.[]; .cursoId == $b)' <<<"$UG")" == true ]] || fail 'Admin no ve usuarios de ambos cursos'
ok 'Directorio de usuarios respeta las reglas por curso'

SECT_B="$(get "$B" '/api/sectores' | jq -er '.[0].id')"
check_denied "$A" "/api/sectores/$SECT_B" '7-A NO puede consultar un sector de 7-B'

# Verificacion de que los comentarios se devuelven desde la API, no solo Room.
if jq -e 'any(.[]; .comentarioEstudiante != null and (.comentarioEstudiante | length > 0))' <<<"$TG" >/dev/null; then
  ok 'El administrador recibe justificaciones persistidas de evidencias'
else
  echo '⚠️ Todavía no hay justificaciones cargadas (envía una evidencia o reimporta el SQL de demo)'
fi

echo
printf 'PRUEBAS COMPLETADAS: %s verificaciones exitosas.\n' "$PASS"
