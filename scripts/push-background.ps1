<#
.SYNOPSIS
    Sube un video a la tele y lo deja como fondo de Consola (reemplaza el anterior).

.DESCRIPTION
    Antes de subirlo lo convierte a 1080p H.264 sin audio, si encuentra ffmpeg.
    Los videos que bajás de YouTube suelen ser WebM/VP9 en 4K aunque se llamen .mp4:
    la interfaz se dibuja a 1080p, así que el 4K es trabajo que no se ve.
    Instalá ffmpeg con: winget install Gyan.FFmpeg.Essentials

.EXAMPLE
    .\push-background.ps1 -Device 192.168.1.50:5555 -Video C:\Videos\fondo.mp4
    .\push-background.ps1 -Device 192.168.1.50:5555 -Video C:\Videos\fondo.mp4 -SkipConvert
#>
param(
    [Parameter(Mandatory)] [string] $Device,
    [Parameter(Mandatory)] [string] $Video,

    # Sube el archivo tal cual, sin convertirlo.
    [switch] $SkipConvert
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

function Find-Ffmpeg {
    $onPath = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    # winget instala acá; el PATH nuevo recién aparece en terminales que abras después de instalar.
    $winget = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Gyan.FFmpeg*\*\bin\ffmpeg.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($winget) { return $winget.FullName }
    return $null
}

function Convert-ToTvVideo([string] $Source) {
    $ffmpeg = Find-Ffmpeg
    if (-not $ffmpeg) {
        Write-Warning "No encontré ffmpeg: subo el video sin convertir. Para convertirlo: winget install Gyan.FFmpeg.Essentials"
        return $Source
    }

    $target = Join-Path $env:TEMP 'consola-fondo-1080p.mp4'
    Write-Host "Convirtiendo a 1080p H.264..." -ForegroundColor Cyan
    # increase + crop = llena la pantalla recortando (igual que el zoom del reproductor), para videos que no son 16:9.
    $ErrorActionPreference = 'Continue'
    & $ffmpeg -hide_banner -loglevel error -y -i $Source `
        -vf 'scale=1920:1080:force_original_aspect_ratio=increase:flags=lanczos,crop=1920:1080' `
        -c:v libx264 -profile:v high -level 4.1 -preset slow -crf 20 -pix_fmt yuv420p `
        -an -movflags +faststart $target
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg no pudo convertir $Source" }
    return $target
}

Assert-DeviceConnected

if (-not (Test-Path -LiteralPath $Video -PathType Leaf)) {
    throw "No encuentro el archivo: $Video"
}

$toUpload = if ($SkipConvert) { $Video } else { Convert-ToTvVideo $Video }
$extension = [System.IO.Path]::GetExtension($toUpload)
$remoteDir = "/sdcard/Android/data/$LauncherPackage/files/backgrounds"

# Consola usa un solo video (el primero alfabéticamente): vaciamos la carpeta para que gane el nuevo.
Invoke-Adb shell mkdir '-p' $remoteDir | Out-Null
Invoke-Adb shell rm '-f' "$remoteDir/*" | Out-Null
Invoke-Adb push $toUpload "$remoteDir/fondo$extension" | Out-Null

Write-Host "Listo. Apretá Home en el control y el fondo nuevo se carga solo." -ForegroundColor Cyan
