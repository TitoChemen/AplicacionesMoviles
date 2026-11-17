# Ejecutar con los 4 microservicios encendidos: powershell -ExecutionPolicy Bypass -File .\scripts\probar_aislamiento_cursos.ps1
param([string]$BaseUrl = "http://localhost:8080")
$ErrorActionPreference = 'Stop'

function Login([string]$correo, [string]$clave) {
    $body = @{ correo=$correo; contrasena=$clave } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType 'application/json' -Body $body
    if (-not $r.accessToken) { throw "Login sin JWT: $correo" }
    return @{ Authorization = "Bearer $($r.accessToken)" }
}
function Get([string]$url, $headers) {
    return Invoke-RestMethod -Uri "$BaseUrl$url" -Method Get -Headers $headers
}
function Assert([bool]$exp, [string]$mensaje) {
    if (-not $exp) { throw "FALLO: $mensaje" }
    Write-Host "OK: $mensaje" -ForegroundColor Green
}
function Denegado([string]$url, $headers) {
    try { $null = Get $url $headers; return $false }
    catch {
        $code = [int]$_.Exception.Response.StatusCode
        return $code -eq 403 -or $code -eq 404
    }
}
$juan = Login 'profesor@huerta.cl' 'Profesor123!'
$maria = Login 'maria.docente@huerta.cl' 'Docente2026!'
$kevin = Login 'estudiante@huerta.cl' 'Estudiante123!'
$listaA = @(Get '/api/tareas' $juan)
$listaB = @(Get '/api/tareas' $maria)
$listaK = @(Get '/api/tareas' $kevin)
Assert ($listaA.Count -gt 0) 'Juan tiene tareas del 7-A'
Assert ($listaB.Count -gt 0) 'Maria tiene tareas del 7-B'
$cursoA = (Get '/api/usuarios/me' $juan).cursoId
$cursoB = (Get '/api/usuarios/me' $maria).cursoId
Assert ($cursoA -ne $cursoB) 'Docentes pertenecen a cursos distintos'
Assert (@($listaA | Where-Object { $_.cursoId -ne $cursoA }).Count -eq 0) 'Juan no recibe tareas de otros cursos'
Assert (@($listaB | Where-Object { $_.cursoId -ne $cursoB }).Count -eq 0) 'Maria no recibe tareas de otros cursos'
Assert (@($listaK | Where-Object { $_.estudianteId -ne (Get '/api/usuarios/me' $kevin).id }).Count -eq 0) 'Estudiante solo ve sus tareas'
$tareaA = $listaA | Select-Object -First 1
$tareaB = $listaB | Select-Object -First 1
Assert (Denegado "/api/tareas/$($tareaB.id)" $juan) 'Juan no puede leer tarea por ID de 7-B'
Assert (Denegado "/api/tareas/$($tareaA.id)" $maria) 'Maria no puede leer tarea por ID de 7-A'
Assert (Denegado "/api/tareas/$($tareaB.id)/evidencias/archivo?nombre=no-existe.jpg" $juan) 'Juan no puede descargar evidencia de 7-B'
$secB = @(Get '/api/sectores' $maria) | Select-Object -First 1
Assert (Denegado "/api/sectores/$($secB.id)" $juan) 'Juan no puede ver sectores de 7-B'
Write-Host 'PRUEBAS BASICAS COMPLETADAS. Verificar tambien PUT/PATCH/DELETE cruzados manualmente.' -ForegroundColor Cyan
