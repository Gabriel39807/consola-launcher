<#
.SYNOPSIS
    Deshace debloat.ps1: vuelve a activar todo lo que se haya desactivado.

.EXAMPLE
    .\restore.ps1 -Device 192.168.1.50:5555
    .\restore.ps1 -Device 192.168.1.50:5555 -UninstallLauncher
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)] [string] $Device,

    # Además desinstala Consola, así el botón Home vuelve sí o sí al inicio de Google.
    [switch] $UninstallLauncher
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

Assert-DeviceConnected

foreach ($package in @($GoogleHome, $SetupWraith, $AmbientMode)) {
    if (-not (Test-PackageInstalled $package)) { continue }
    if ($PSCmdlet.ShouldProcess($package, 'pm enable')) {
        Invoke-Adb shell pm enable '--user' 0 $package | Out-Null
        Write-Host "Activado: $package" -ForegroundColor Green
    }
}

# El protector de pantalla vuelve al de fábrica.
if ($PSCmdlet.ShouldProcess('screensaver_components', 'protector de fábrica')) {
    $factory = @(Invoke-Adb shell settings get secure screensaver_default_component)[0]
    if ($factory -and $factory -ne 'null') {
        Invoke-Adb shell settings put secure screensaver_components $factory | Out-Null
        Write-Host "Protector de pantalla: el de fábrica" -ForegroundColor Green
    }
}
if ((Test-PackageInstalled $LauncherPackage) -and $PSCmdlet.ShouldProcess($MediaListenerComponent, 'cmd notification disallow_listener')) {
    Invoke-Adb shell cmd notification disallow_listener $MediaListenerComponent | Out-Null
}
if ($PSCmdlet.ShouldProcess($SettingsKeyComponent, 'desactivar servicio de accesibilidad')) {
    $services = @(Get-SecureList 'enabled_accessibility_services' | Where-Object { $_ -ne $SettingsKeyComponent })
    Set-SecureList 'enabled_accessibility_services' $services
    if ($services.Count -eq 0) { Invoke-Adb shell settings put secure accessibility_enabled 0 | Out-Null }
}
if ((Test-PackageInstalled $LauncherPackage) -and $PSCmdlet.ShouldProcess($LauncherPackage, 'pm revoke WRITE_SECURE_SETTINGS')) {
    Invoke-Adb shell pm revoke $LauncherPackage android.permission.WRITE_SECURE_SETTINGS | Out-Null
}

if ($UninstallLauncher -and $PSCmdlet.ShouldProcess($LauncherPackage, 'adb uninstall')) {
    Invoke-Adb uninstall $LauncherPackage | Out-Null
    Write-Host "Desinstalado: $LauncherPackage" -ForegroundColor Green
}

Invoke-Adb shell input keyevent KEYCODE_HOME | Out-Null
Write-Host "Listo. Si la tele te pregunta qué inicio usar, elegí Google TV." -ForegroundColor Cyan
