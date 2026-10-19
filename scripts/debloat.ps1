<#
.SYNOPSIS
    Desactiva el inicio de Google TV y deja Consola como launcher por defecto.

.DESCRIPTION
    Usa "pm disable-user", que es REVERSIBLE: no borra nada (ver restore.ps1).
    Probá primero con -WhatIf para ver qué haría sin tocar la tele.

.EXAMPLE
    .\debloat.ps1 -Device 192.168.1.50:5555 -WhatIf
    .\debloat.ps1 -Device 192.168.1.50:5555
    .\debloat.ps1 -Device 192.168.1.50:5555 -IncludeSetupWraith
    .\debloat.ps1 -Device 192.168.1.50:5555 -KeepGoogleScreensaver
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)] [string] $Device,

    # Usalo SOLO si después de la primera pasada el botón Home te sigue llevando a una pantalla de Google.
    [switch] $IncludeSetupWraith,

    # Desactiva el protector de pantalla de Google (el que muestra recomendaciones).
    [switch] $IncludeAmbientMode,

    # No cambia el protector de pantalla (igual se puede activar después desde Ajustes → Sistema).
    [switch] $KeepGoogleScreensaver
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

Assert-DeviceConnected

# Si desactivás el home de Google SIN tener otro instalado, la tele no tiene a dónde volver: pantalla negra.
if (-not (Test-PackageInstalled $LauncherPackage)) {
    throw "Consola no está instalado en la tele. Instalalo desde Android Studio ANTES de correr esto."
}

$targets = @($GoogleHome)
if ($IncludeSetupWraith) { $targets += $SetupWraith }
if ($IncludeAmbientMode) { $targets += $AmbientMode }

foreach ($package in $targets) {
    if (-not (Test-PackageInstalled $package)) {
        Write-Warning "$package no existe en esta tele, lo salteo."
        continue
    }
    if ($PSCmdlet.ShouldProcess($package, 'pm disable-user')) {
        Invoke-Adb shell pm disable-user '--user' 0 $package | Out-Null
        Write-Host "Desactivado: $package" -ForegroundColor Green
    }
}

# Google TV no tiene pantalla para elegir el protector. Con este permiso, Consola lo cambia desde
# Ajustes → Sistema. Es un permiso que solo se puede dar por ADB: por eso va acá.
if ($PSCmdlet.ShouldProcess($LauncherPackage, 'pm grant WRITE_SECURE_SETTINGS')) {
    Invoke-Adb shell pm grant $LauncherPackage android.permission.WRITE_SECURE_SETTINGS | Out-Null
    Write-Host "Permiso para elegir el protector de pantalla: concedido" -ForegroundColor Green
}
if (-not $KeepGoogleScreensaver -and $PSCmdlet.ShouldProcess($DreamComponent, 'protector de pantalla')) {
    Invoke-Adb shell settings put secure screensaver_components $DreamComponent | Out-Null
    Write-Host "Protector de pantalla: tu fondo con la hora" -ForegroundColor Green
}

if ($PSCmdlet.ShouldProcess($LauncherComponent, 'cmd package set-home-activity')) {
    Invoke-Adb shell cmd package set-home-activity $LauncherComponent | Out-Null
    Invoke-Adb shell input keyevent KEYCODE_HOME | Out-Null
    Write-Host "Listo. Apretá Home en el control: tendría que abrir Consola." -ForegroundColor Cyan
    Write-Host "Si algo salió mal: .\restore.ps1 -Device $Device"
}
