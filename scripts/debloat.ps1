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
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)] [string] $Device,

    # Usalo SOLO si después de la primera pasada el botón Home te sigue llevando a una pantalla de Google.
    [switch] $IncludeSetupWraith,

    # Desactiva el protector de pantalla de Google (el que muestra recomendaciones).
    [switch] $IncludeAmbientMode
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

if ($PSCmdlet.ShouldProcess($LauncherComponent, 'cmd package set-home-activity')) {
    Invoke-Adb shell cmd package set-home-activity $LauncherComponent | Out-Null
    Invoke-Adb shell input keyevent KEYCODE_HOME | Out-Null
    Write-Host "Listo. Apretá Home en el control: tendría que abrir Consola." -ForegroundColor Cyan
    Write-Host "Si algo salió mal: .\restore.ps1 -Device $Device"
}
