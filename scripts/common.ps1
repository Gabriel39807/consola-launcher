# Funciones compartidas. Se carga con: . "$PSScriptRoot\common.ps1"
# Requiere que el script que lo carga tenga un parámetro $Device (ej: 192.168.1.50:5555).

$LauncherPackage = 'com.consola.launcher'
$LauncherComponent = "$LauncherPackage/.MainActivity"

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
