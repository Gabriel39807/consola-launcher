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

if ($UninstallLauncher -and $PSCmdlet.ShouldProcess($LauncherPackage, 'adb uninstall')) {
    Invoke-Adb uninstall $LauncherPackage | Out-Null
    Write-Host "Desinstalado: $LauncherPackage" -ForegroundColor Green
}

Invoke-Adb shell input keyevent KEYCODE_HOME | Out-Null
Write-Host "Listo. Si la tele te pregunta qué inicio usar, elegí Google TV." -ForegroundColor Cyan
