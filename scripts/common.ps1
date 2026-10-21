# Funciones compartidas. Se carga con: . "$PSScriptRoot\common.ps1"
# Requiere que el script que lo carga tenga un parámetro $Device (ej: 192.168.1.50:5555).

$LauncherPackage = 'com.consola.launcher'
$LauncherComponent = "$LauncherPackage/.MainActivity"
$DreamComponent = "$LauncherPackage/.dream.BackgroundDream"
$MediaListenerComponent = "$LauncherPackage/com.consola.launcher.widgets.MediaListener"
$SettingsKeyComponent = "$LauncherPackage/com.consola.launcher.keys.SettingsKeyService"

# Paquetes que se pueden desactivar. TODOS son reversibles con restore.ps1.
$GoogleHome = 'com.google.android.apps.tv.launcherx'        # El inicio de Google TV: recomendaciones y publicidad
$SetupWraith = 'com.google.android.tungsten.setupwraith'    # En algunos firmwares, el Home cae acá si falta launcherx
$AmbientMode = 'com.google.android.apps.tv.dreamx'          # Protector de pantalla de Google con recomendaciones

function Invoke-Adb {
    # En Windows PowerShell 5.1, si adb escribe en stderr y la preferencia es 'Stop', el script se corta
    # aunque adb haya terminado bien. Por eso decidimos por el código de salida, no por stderr.
    $ErrorActionPreference = 'Continue'
    $output = & adb -s $Device @args 2>&1 | ForEach-Object { "$_".Trim() }
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($args -join ' ') falló:`n$($output -join "`n")"
    }
    $output
}

# Android guarda los servicios de accesibilidad activos como "a/b:c/d". Agregamos o sacamos el nuestro
# SIN pisar los que ya tenga la tele (un lector de pantalla, por ejemplo).
function Get-SecureList([string] $Key) {
    $value = @(Invoke-Adb shell settings get secure $Key)[0]
    if (-not $value -or $value -eq 'null') { return @() }
    return @($value -split ':' | Where-Object { $_ })
}

function Set-SecureList([string] $Key, [string[]] $Items) {
    if ($Items.Count -eq 0) {
        Invoke-Adb shell settings delete secure $Key | Out-Null
    } else {
        Invoke-Adb shell settings put secure $Key ($Items -join ':') | Out-Null
    }
}

function Test-PackageInstalled([string] $Package) {
    $packages = Invoke-Adb shell pm list packages $Package
    return @($packages) -contains "package:$Package"
}

function Assert-DeviceConnected {
    $devices = & adb devices | Select-String -SimpleMatch $Device
    if (-not $devices) {
        throw "No veo la tele $Device. Corré primero: adb connect $Device"
    }
}
