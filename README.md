# Consola Launcher

Un inicio para Google TV con estilo de consola (PS5/Xbox): una fila de apps, el foco que agranda el tile, un título grande abajo y un **fondo animado**. El fondo es un video en loop o, si no subiste ninguno, una aurora animada generada por código.

Pensado para: **Challenger 4K con Google TV, Android TV 14**.

---

## Antes de tocar código: 3 conceptos

### 1. ¿Qué ES un launcher?
Es una app normal y corriente. No hay ninguna magia. Lo único que la hace especial es este `intent-filter` del [AndroidManifest.xml](app/src/main/AndroidManifest.xml):

```xml
<category android:name="android.intent.category.HOME" />
```

Cuando apretás Home, Android busca qué Activity declara `HOME` y la abre. Google TV es UNA de esas apps (`com.google.android.apps.tv.launcherx`). Si la desactivás, la nuestra pasa a ser la única candidata. Así de simple.

### 2. En la tele, el FOCO es todo
En el celular tocás la pantalla. En la tele no hay dedos: tenés un D-pad (las flechas del control). Siempre hay **un** elemento enfocado, y las flechas mueven el foco. Si nadie tiene el foco, el control no hace NADA. Por eso:
- [AppTile.kt](app/src/main/java/com/consola/launcher/home/AppTile.kt) dibuja el foco (escala + borde).
- [HomeScreen.kt](app/src/main/java/com/consola/launcher/home/HomeScreen.kt) le da el foco al primer tile al arrancar.
- [VideoBackground.kt](app/src/main/java/com/consola/launcher/background/VideoBackground.kt) marca el reproductor como NO enfocable, porque si no, el reproductor se queda con las flechas.

### 3. Los decodificadores de video son un recurso escaso
Una tele económica tiene muy pocos decodificadores de video por hardware. Si tu fondo se queda con uno mientras mirás Netflix, Netflix puede quedarse sin él. Por eso el reproductor se **crea** cuando el inicio aparece y se **libera** cuando se oculta (`LifecycleStartEffect`). No es un detalle: es lo que separa un launcher usable de uno que te rompe las otras apps.

---

## Estructura (Screaming Architecture: las carpetas dicen QUÉ hace la app)

```
app/src/main/java/com/consola/launcher/
├── apps/          → qué apps hay y en qué orden
│   ├── AppCatalog.kt          ← regla de negocio pura (con tests)
│   └── InstalledAppsSource.kt ← adaptador al PackageManager
├── background/    → fondos animados (video + aurora)
├── home/          → la pantalla de inicio
├── ui/            → tema
└── LauncherConfig.kt ← TUS favoritos y apps ocultas
```

`AppCatalog` no sabe nada de Android. ¿Y sabés por qué? Porque así lo testeás en milisegundos, sin emulador: [AppCatalogTest.kt](app/src/test/java/com/consola/launcher/apps/AppCatalogTest.kt).

---

## Paso 1: Preparar la tele

1. **Ajustes → Sistema → Acerca de** → apretá **7 veces** sobre *Compilación del SO de Android TV* hasta que diga que sos desarrollador.
2. **Ajustes → Sistema → Opciones para desarrolladores** → activá **Depuración USB** y, si aparece, **Depuración inalámbrica**.
3. Anotá la IP de la tele (**Ajustes → Red e Internet** → tu red).

En la PC instalá las herramientas de ADB:

```bash
winget install Google.PlatformTools
```

Conectate. En Android TV 14 normalmente hay que **emparejar** primero: la tele te muestra IP, puerto y un código en *Depuración inalámbrica → Vincular dispositivo*.

```bash
adb pair 192.168.1.50:37123
```

```bash
adb connect 192.168.1.50:5555
```

> El puerto de `pair` y el de `connect` suelen ser DISTINTOS: los dos te los muestra la tele. Aceptá el diálogo "¿Permitir depuración?" en la pantalla.

## Paso 2: Compilar e instalar

1. Instalá [Android Studio](https://developer.android.com/studio).
2. **File → Open** → elegí la carpeta `consola-launcher`. Esperá a que termine el *Gradle Sync*.
3. Arriba, en el selector de dispositivos, debería aparecer tu tele (por estar conectada por ADB). Dale **Run ▶**.

Para correr los tests: clic derecho en `AppCatalogTest` → **Run**.

## Paso 3: Probalo SIN tocar nada del sistema

Consola aparece como una app más en la tele. Abrila desde ahí y movete con el control. Si algo no te gusta, ajustalo ACÁ, cuando todavía no reemplazaste nada. Primero se prueban las paredes, después te mudás.

## Paso 4: Tu fondo animado

Desde PowerShell, en la carpeta `scripts`:

```bash
powershell -ExecutionPolicy Bypass -File .\push-background.ps1 -Device 192.168.1.50:5555 -Video C:\Videos\fondo.mp4
```

El script **convierte el video solo** a 1080p H.264 sin audio, si tenés ffmpeg (`winget install Gyan.FFmpeg.Essentials`). Ojo: los videos que bajás de YouTube suelen ser **WebM/VP9 en 4K aunque se llamen `.mp4`**. La interfaz se dibuja a 1080p, así que el 4K es trabajo que no se ve.

**Recomendaciones para el video:**
- Entre 10 y 30 segundos, pensado para hacer un loop sin corte visible.
- Oscuro o de colores apagados: el texto blanco tiene que leerse encima.

> **Por qué el video usa TextureView y no SurfaceView** (medido en una Challenger con Mali-G52): con SurfaceView, el video queda en una capa aparte que el hardware de la tele no puede mostrar debajo de la interfaz. El sistema la compone con la GPU, escalada a la salida 4K en cada frame (`Comp Type: CLIENT` en `dumpsys SurfaceFlinger`), y navegando el 100 % de los frames salía trabado. Con TextureView, el video es una textura más dentro de la app: queda una sola capa por hardware y solo el **0,75 %** de los frames sale trabado. Ver [background_player.xml](app/src/main/res/layout/background_player.xml).

Si no subís ningún video, o si el video no se puede reproducir, se usa la aurora animada. Nunca te queda una pantalla negra.

## Paso 5: Sacar el inicio de Google

Primero mirá lo que haría, sin tocar nada:

```bash
powershell -ExecutionPolicy Bypass -File .\debloat.ps1 -Device 192.168.1.50:5555 -WhatIf
```

Y ahora sí:

```bash
powershell -ExecutionPolicy Bypass -File .\debloat.ps1 -Device 192.168.1.50:5555
```

Apretá Home. ¿Abre Consola? Listo, ya está.

**¿Te sigue llevando a una pantalla de Google?** Algunos firmwares mandan el Home a otra pantalla cuando falta el inicio de Google. Probá:

```bash
powershell -ExecutionPolicy Bypass -File .\debloat.ps1 -Device 192.168.1.50:5555 -IncludeSetupWraith
```

> ⚠️ Después de hacer esto, probá que el botón de **entradas/HDMI** del control siga funcionando. En algunas teles con Google TV, el selector de entradas vive dentro del inicio de Google.

## Si algo sale mal

Todo es reversible:

```bash
powershell -ExecutionPolicy Bypass -File .\restore.ps1 -Device 192.168.1.50:5555
```

Con `-UninstallLauncher` además desinstala Consola. Y en el peor de los casos, un reset de fábrica vuelve a activar todo.

---

## Personalizar

- **Favoritos y apps ocultas:** [LauncherConfig.kt](app/src/main/java/com/consola/launcher/LauncherConfig.kt). Para saber el nombre del paquete de una app:

```bash
adb shell pm list packages
```

- **Tamaño de los tiles, escala del foco:** las constantes al principio de [AppTile.kt](app/src/main/java/com/consola/launcher/home/AppTile.kt).
- **Colores de la aurora:** las constantes al principio de [AuroraBackground.kt](app/src/main/java/com/consola/launcher/background/AuroraBackground.kt).

## Ideas para seguir (en orden de dificultad)

1. Un fondo distinto para cada app enfocada (como en la PS5 con cada juego).
2. Una pantalla de ajustes dentro del launcher para elegir favoritos sin recompilar.
3. Varias filas (Favoritos / Todas / Juegos).
4. Un plan B para el botón Home, por si un update de Google lo rompe: un servicio de accesibilidad que detecte cuando aparece el inicio de Google y abra Consola.
