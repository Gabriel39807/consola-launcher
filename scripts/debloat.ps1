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

    # Ya no hace falta: el script lo detecta solo. Queda para forzarlo.
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

# Android elige el Home por PRIORIDAD antes que por preferencia: el asistente de configuración de Google
# (prioridad 1) le gana a Consola (0) aunque se lo marque como preferido. Si después de sacar el inicio de
# Google el Home cae ahí, lo desactivamos también, sin que haya que adivinar el parámetro.
if (-not $IncludeSetupWraith -and -not $WhatIfPreference) {
    $resolved = Invoke-Adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
    if ((@($resolved) -join "`n") -match [regex]::Escape($SetupWraith)) {
        Invoke-Adb shell pm disable-user '--user' 0 $SetupWraith | Out-Null
        Write-Host "Desactivado: $SetupWraith (el Home caía en el asistente de configuración)" -ForegroundColor Green
    }
}

# Google TV no tiene pantalla para elegir el protector. Con este permiso, Consola lo cambia desde
# Ajustes → Sistema. Es un permiso que solo se puede dar por ADB: por eso va acá.
if ($PSCmdlet.ShouldProcess($LauncherPackage, 'pm grant WRITE_SECURE_SETTINGS')) {
    Invoke-Adb shell pm grant $LauncherPackage android.permission.WRITE_SECURE_SETTINGS | Out-Null
    Write-Host "Permiso para elegir el protector de pantalla: concedido" -ForegroundColor Green
}
# El widget "Reproduciendo ahora" necesita ver las sesiones de medios (también se puede dar desde la tele).
if ($PSCmdlet.ShouldProcess($MediaListenerComponent, 'cmd notification allow_listener')) {
    Invoke-Adb shell cmd notification allow_listener $MediaListenerComponent | Out-Null
    Write-Host "Widget de música: habilitado" -ForegroundColor Green
}
# El botón Ajustes del control abría un panel que vive dentro del inicio de Google. Sin él, no hacía nada:
# este servicio de Consola lo atrapa y abre los ajustes de la tele.
if ($PSCmdlet.ShouldProcess($SettingsKeyComponent, 'activar servicio de accesibilidad')) {
    $services = Get-SecureList 'enabled_accessibility_services'
    if ($services -notcontains $SettingsKeyComponent) {
        Set-SecureList 'enabled_accessibility_services' ($services + $SettingsKeyComponent)
    }
    Invoke-Adb shell settings put secure accessibility_enabled 1 | Out-Null
    Write-Host "Botón Ajustes del control: habilitado" -ForegroundColor Green
}
if (-not $KeepGoogleScreensaver -and $PSCmdlet.ShouldProcess($DreamComponent, 'protector de pantalla')) {
    Invoke-Adb shell settings put secure screensaver_components $DreamComponent | Out-Null
    Write-Host "Protector de pantalla: tu fondo con la hora" -ForegroundColor Green
}

# Compila Consola por adelantado: sin esto, las primeras veces Android la interpreta y el panel y las
# animaciones se sienten trabados hasta que "entra en calor".
if ($PSCmdlet.ShouldProcess($LauncherPackage, 'cmd package compile -m speed')) {
    Invoke-Adb shell cmd package compile -m speed -f $LauncherPackage | Out-Null
    Write-Host "Consola compilada para que vaya fluida" -ForegroundColor Green
}

if ($PSCmdlet.ShouldProcess($LauncherComponent, 'cmd package set-home-activity')) {
    Invoke-Adb shell cmd package set-home-activity $LauncherComponent | Out-Null
    Invoke-Adb shell input keyevent KEYCODE_HOME | Out-Null
    Write-Host "Listo. Apretá Home en el control: tendría que abrir Consola." -ForegroundColor Cyan
    Write-Host "Si algo salió mal: .\restore.ps1 -Device $Device"
}
