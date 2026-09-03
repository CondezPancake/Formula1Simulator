# Descarga Piper (motor de voz) y la voz en español que usa la radio del
# equipo. Se hace en un script aparte -y no al vuelo desde TtsManager- por lo
# mismo que el wrapper de Maven descarga su distribución una sola vez: son
# ~100 MB de binarios que no tiene sentido llevar en el repo (ver
# .gitignore), pero tampoco descargar en cada arranque de la app.
#
# Idempotente: si tools/piper/bin/piper.exe ya existe, no vuelve a descargar
# nada. Uso: desde la raíz del repo, "powershell -File tools/setup_piper.ps1".

$ErrorActionPreference = "Stop"

$raiz = Split-Path -Parent $PSScriptRoot
$piperDir = Join-Path $raiz "tools\piper"
$binDir = Join-Path $piperDir "bin"
$vocesDir = Join-Path $piperDir "voices"

if (Test-Path (Join-Path $binDir "piper.exe")) {
    Write-Host "Piper ya está instalado en $binDir. Nada que hacer."
    exit 0
}

New-Item -ItemType Directory -Force -Path $binDir, $vocesDir | Out-Null

$piperZipUrl = "https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_windows_amd64.zip"
$piperZip = Join-Path $env:TEMP "piper_windows_amd64.zip"

Write-Host "Descargando Piper..."
Invoke-WebRequest -Uri $piperZipUrl -OutFile $piperZip

Write-Host "Extrayendo Piper en $binDir..."
$extraido = Join-Path $env:TEMP "piper_extraido"
if (Test-Path $extraido) { Remove-Item -Recurse -Force $extraido }
Expand-Archive -Path $piperZip -DestinationPath $extraido
Copy-Item -Path (Join-Path $extraido "piper\*") -Destination $binDir -Recurse -Force
Remove-Item -Recurse -Force $extraido
Remove-Item -Force $piperZip

# es_ES-davefx-medium: voz masculina de España, calidad media (~60 MB). Buen
# punto medio entre naturalidad y tamaño de descarga para un ingeniero de
# pista; ver https://github.com/rhasspy/piper/blob/master/VOICES.md para
# otras voces en español si se prefiere cambiarla.
$vozBase = "https://huggingface.co/rhasspy/piper-voices/resolve/main/es/es_ES/davefx/medium"
Write-Host "Descargando voz en español (es_ES-davefx-medium, ~60 MB)..."
Invoke-WebRequest -Uri "$vozBase/es_ES-davefx-medium.onnx" -OutFile (Join-Path $vocesDir "es_ES-davefx-medium.onnx")
Invoke-WebRequest -Uri "$vozBase/es_ES-davefx-medium.onnx.json" -OutFile (Join-Path $vocesDir "es_ES-davefx-medium.onnx.json")

Write-Host "Listo. Piper instalado en $piperDir."
