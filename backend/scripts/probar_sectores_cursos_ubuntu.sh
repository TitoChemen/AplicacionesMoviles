#!/usr/bin/env bash
# Prueba integrada con mutaciones temporales: requiere SQL de demo y los 4 servicios.
set -euo pipefail
API_URL="${API_URL:-http://localhost:8080}"
command -v curl >/dev/null && command -v jq >/dev/null || { echo 'Instala curl y jq' >&2; exit 1; }
temp="$(mktemp)"
sector_id=''
admin=''
cleanup() {
  if [[ -n "$sector_id" && -n "$admin" ]]; then
    curl -sS --max-time 10 -o /dev/null -X DELETE -H "Authorization: Bearer $admin" \
      "$API_URL/api/sectores/$sector_id" || true
  fi
  rm -f "$temp"
}
trap cleanup EXIT
login() {
  curl -fsS -H 'Content-Type: application/json' -X POST "$API_URL/api/auth/login" \
    -d "$(jq -nc --arg correo "$1" --arg contrasena "$2" '{correo:$correo,contrasena:$contrasena}')" \
    | jq -er '.accessToken'
}
status() {
  local token="$1" method="$2" path="$3" body="${4:-}"
  if [[ -n "$body" ]]; then
    curl -sS --max-time 15 -o "$temp" -w '%{http_code}' -X "$method" \
      -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
      -d "$body" "$API_URL$path"
  else
    curl -sS --max-time 15 -o "$temp" -w '%{http_code}' -X "$method" \
      -H "Authorization: Bearer $token" "$API_URL$path"
  fi
}
check() { [[ "$1" == "$2" ]] || { echo "FALLO: $3 (HTTP $1, esperado $2)" >&2; cat "$temp" >&2; exit 1; }; echo "OK: $3"; }

admin="$(login 'admin.global@huerta.cl' 'AdminGlobal2026!')"
docente="$(login 'profesor@huerta.cl' 'Profesor123!')"
alumno="$(login 'estudiante@huerta.cl' 'Estudiante123!')"
cursos="$(curl -fsS -H "Authorization: Bearer $admin" "$API_URL/api/cursos")"
curso_a="$(jq -er '.[] | select(.codigo == "7-A") | .id' <<<"$cursos")"
curso_b="$(jq -er '.[] | select(.codigo == "7-B") | .id' <<<"$cursos")"
codigo="TEST-$(date +%s)-$RANDOM"
payload="$(jq -nc --arg codigo "$codigo" --argjson cursoId "$curso_a" \
  '{codigo:$codigo,nombre:"Prueba aislada",descripcion:"Temporal",ubicacion:"Patio",estado:"ACTIVO",cursoId:$cursoId}')"

check "$(status "$docente" POST '/api/sectores' "$payload")" 403 'Docente no puede crear un sector'
check "$(status "$admin" POST '/api/sectores' "$payload")" 201 'Administrador crea sector en 7-A'
sector_id="$(jq -er '.id' "$temp")"
check "$(status "$docente" GET "/api/sectores/$sector_id")" 200 'Docente 7-A lee el sector'
check "$(status "$alumno" GET "/api/sectores/$sector_id")" 200 'Estudiante 7-A lee el sector'

payload="$(jq -nc --arg codigo "$codigo" --argjson cursoId "$curso_b" \
  '{codigo:$codigo,nombre:"Prueba aislada",descripcion:"Temporal",ubicacion:"Patio",estado:"ACTIVO",cursoId:$cursoId}')"
check "$(status "$docente" PUT "/api/sectores/$sector_id" "$payload")" 403 'Docente no puede reasignar curso'
check "$(status "$admin" PUT "/api/sectores/$sector_id" "$payload")" 200 'Administrador traslada sector a 7-B'
check "$(status "$docente" GET "/api/sectores/$sector_id")" 403 'Docente 7-A pierde acceso'
check "$(status "$alumno" GET "/api/sectores/$sector_id")" 403 'Estudiante 7-A pierde acceso'
lista_b="$(curl -fsS -H "Authorization: Bearer $(login 'profesor.7b@huerta.cl' 'Profesor7B2026!')" "$API_URL/api/sectores")"
jq -e --argjson id "$sector_id" 'any(.[]; .id == $id)' <<<"$lista_b" >/dev/null || {
  echo 'FALLO: 7-B no recibió el sector trasladado' >&2; exit 1;
}
echo 'OK: Docente 7-B recibe el sector trasladado'
check "$(status "$admin" DELETE "/api/sectores/$sector_id")" 204 'Administrador elimina sector temporal'
sector_id=''

# Un sector con tareas persistidas no se puede mover: la tarea conservaría su curso anterior.
tarea="$(curl -fsS -H "Authorization: Bearer $admin" "$API_URL/api/tareas" | jq -er '.[0]')"
sector_con_tarea="$(jq -er '.sectorId' <<<"$tarea")"
check "$(status "$admin" GET "/api/sectores/$sector_con_tarea")" 200 'Consulta sector con tarea'
curso_origen="$(jq -er '.cursoId' "$temp")"
curso_destino="$curso_a"
[[ "$curso_origen" == "$curso_a" ]] && curso_destino="$curso_b"
payload="$(jq --argjson cursoId "$curso_destino" '.cursoId = $cursoId' "$temp")"
check "$(status "$admin" PUT "/api/sectores/$sector_con_tarea" "$payload")" 409 \
  'No traslada un sector con tareas o evidencias'
