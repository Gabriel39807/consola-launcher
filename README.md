# Consola Launcher

Un inicio para Google TV con estilo de consola: pestañas **Buscar · Inicio · Apps · Ajustes**, un título grande de la app enfocada, una fila de favoritas y un **fondo animado**. El fondo es un video en loop que subís **desde el celular con un QR**, o una aurora animada que toma el color de la app enfocada.

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
├── background/    → fondos animados (video + aurora) y la biblioteca de videos
├── home/          → el armazón: pestañas, Inicio y Apps
├── search/        → la pestaña Buscar (abre el Asistente de Google)
├── settings/      → Ajustes: fondos, apps del inicio, sistema (guardado con DataStore)
├── upload/        → subir fondos desde el celular: servidor HTTP, QR y conversión a 1080p
├── dream/         → el protector de pantalla: tu fondo con la hora
├── widgets/       → "Reproduciendo ahora" (Spotify y cualquier app de música) y reloj grande
├── ui/            → tema y márgenes seguros
├── LauncherGraph.kt  ← una instancia de cada cosa para toda la app (DI a mano)
└── LauncherConfig.kt ← favoritas y ocultas POR DEFECTO (después se cambian desde Ajustes)
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

### Desde el celular (lo normal)

En la tele: **Ajustes → Fondos**. Aparece un QR. Escanealo con el celular (tiene que estar en la misma WiFi), elegí un video y listo. La tele lo **adapta sola** a 1080p H.264 con su codificador por hardware (Media3 Transformer) y lo deja como fondo. Desde esa misma página del celular también podés borrar fondos.

Detalles de seguridad:
- El servidor existe **solo mientras Ajustes → Fondos está en pantalla**. Salís, y se cierra.
- La dirección lleva un **código aleatorio** que cambia cada vez. Sin él, el servidor responde 404 a todo.
- Solo acepta videos (mp4, webm, mkv, mov) de hasta 500 MB. El nombre del archivo se limpia, porque lo manda otro dispositivo.

### Desde la PC (por ADB)

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

> **Por qué la tele convierte los videos** (medido): un 4K VP9 como fondo deja la GPU en ~32 ms por frame y **65 %** de frames trabados con el inicio quieto. El mismo video en 1080p H.264: ~10 ms y **0 %**.

## Paso 5: Sacar el inicio de Google

Primero mirá lo que haría, sin tocar nada:

```bash
powershell -ExecutionPolicy Bypass -File .\debloat.ps1 -Device 192.168.1.50:5555 -WhatIf
```

Y ahora sí:

```bash
powershell -ExecutionPolicy Bypass -File .\debloat.ps1 -Device 192.168.1.50:5555
```

Apretá Home: abre Consola. Y al prender la tele también arranca Consola, porque es el único inicio que queda.

> **Por qué a veces hay que sacar dos cosas de Google:** Android elige el Home por *prioridad* antes que por preferencia. En muchas teles el asistente de configuración de Google (`setupwraith`, prioridad 1) le gana a cualquier launcher instalado (prioridad 0). El script lo detecta: si después de sacar el inicio de Google el Home cae en el asistente, lo desactiva también. Con `-IncludeSetupWraith` lo forzás.

> ⚠️ Después de hacer esto, probá que el botón de **entradas/HDMI** del control siga funcionando. En algunas teles con Google TV, el selector de entradas vive dentro del inicio de Google.

## Si algo sale mal

Todo es reversible:

```bash
powershell -ExecutionPolicy Bypass -File .\restore.ps1 -Device 192.168.1.50:5555
```

Con `-UninstallLauncher` además desinstala Consola. Y en el peor de los casos, un reset de fábrica vuelve a activar todo.

---

## Personalizar

- **Favoritas y apps ocultas:** desde la tele, en **Ajustes → Apps del inicio**. Los valores de fábrica están en [LauncherConfig.kt](app/src/main/java/com/consola/launcher/LauncherConfig.kt).
- **Fondo:** en **Ajustes → Fondos** elegís entre la aurora y tus videos.
- **Ordenar el Inicio:** en Inicio, **mantené OK** sobre una app. Se agranda con flechas: movela con ◀ ▶ y soltala con OK (o Atrás).
- **Agregar o sacar del Inicio:** en Apps, **mantené OK** sobre cualquier app. Las que están en Inicio llevan una estrellita.
- **Estilo del Inicio:** en **Ajustes → Estilo** elegís entre **Cine**, **PS5**, **Xbox**, **Minimal** y **Filas** (tus apps agrupadas solas por tipo: Video, Música, Juegos). Todos comparten tus favoritas, el "mantener OK para mover" y los widgets.
- **Reloj:** en **Ajustes → Reloj** cambiás formato (24 h / 12 h), segundos, fecha (completa, corta, solo el día u oculta), grosor y tamaño. Se aplica a la barra, al Inicio Minimal, al widget y al protector de pantalla.
- **Widgets:** en **Ajustes → Widgets** activás **Reproduciendo ahora** (carátula, canción, avance y controles, con el color del disco) y **Reloj grande**. El de música necesita "acceso a notificaciones": la fila "Permitir ver qué está sonando" abre la pantalla de la tele para darlo, o lo da `debloat.ps1`.
- **Temas:** en **Ajustes → Temas** aplicás un look completo con un clic (Cine, Gamer, Zen, Arcade) o guardás el tuyo. Un tema cambia estilo, reloj, widgets, oscurecido, "que baile" y letra, pero NUNCA tus apps ni tus fondos.
- **Letras:** Montserrat, Orbitron, Playfair y Bebas Neue (licencia OFL, en `assets/licenses`), o la de la tele.
- **Fondos vivos:** en **Ajustes → Fondos → Cómo se ve**: cambiar de fondo solo (cada 15, 30 o 60 minutos, o uno de día y otro de noche: mantené OK sobre un fondo para marcarlo), oscurecer el fondo, y **que el fondo baile con la música** (pide permiso de micrófono porque Android lo exige para analizar el audio; no graba nada).
- **Desde el celular:** el QR de Ajustes abre una página con tres pestañas: **Fondos** (subir y borrar videos), **Apps** (cambiar el nombre y subir una portada propia a cada app; se recorta sola a 16:9) y **Protector** (un mensaje debajo de la hora). El servidor vive solo mientras estás en Ajustes.
- **Protector de pantalla:** **Ajustes → Sistema → Protector de pantalla** usa tu mismo fondo con la hora, que cambia de esquina cada minuto para no marcar la pantalla. Google TV no tiene una pantalla para elegir protectores, así que el permiso para cambiarlo se da una sola vez con `debloat.ps1` (si no lo querés, pasale `-KeepGoogleScreensaver`).

- **Tamaño de los tiles, escala del foco:** las constantes al principio de [AppTile.kt](app/src/main/java/com/consola/launcher/home/AppTile.kt).
- **Colores de la aurora:** las constantes al principio de [AuroraBackground.kt](app/src/main/java/com/consola/launcher/background/AuroraBackground.kt).

## Ideas para seguir (en orden de dificultad)

1. Un fondo distinto para cada app enfocada (como en la PS5 con cada juego).
2. Varias filas en Inicio (Favoritas / Recientes / Juegos).
3. Un plan B para el botón Home, por si un update de Google lo rompe: un servicio de accesibilidad que detecte cuando aparece el inicio de Google y abra Consola.

## Cómo baila el fondo

[AudioPulse.kt](app/src/main/java/com/consola/launcher/audio/AudioPulse.kt) usa el `Visualizer` de Android sobre la sesión 0 (la mezcla de todo lo que suena) y [AudioLevels.kt](app/src/main/java/com/consola/launcher/audio/AudioLevels.kt) convierte la FFT en graves, medios y agudos, suavizados para que "suba rápido y baje lento". En la aurora, la luz principal crece con los graves; sobre un video, el degradé se aclara en cada golpe. El pulso se lee en la fase de **dibujo**, no de composición: 20 veces por segundo se redibuja el fondo, no toda la pantalla. Ojo: algunas teles mandan el audio por un camino de hardware que no se puede analizar; en ese caso la opción avisa "Esta tele no deja analizar el audio".

## Cómo sabe el widget qué está sonando

Spotify no le da esa información a otras apps directamente. Pero toda app de música publica una **sesión de medios** para que el control remoto y el sistema la puedan manejar: [NowPlayingSource.kt](app/src/main/java/com/consola/launcher/widgets/NowPlayingSource.kt) lee esa sesión con `MediaSessionManager`. Android solo se la muestra a las apps con acceso a notificaciones, y por eso existe [MediaListener.kt](app/src/main/java/com/consola/launcher/widgets/MediaListener.kt), que no lee ninguna notificación. Android lista solo las sesiones **activas**: si la tele se duerme y la música se corta, el widget muestra "Nada sonando" y un botón para abrir Spotify.

## Mantener OK: por qué no alcanza con `clickable()`

`clickable()` de Compose no distingue "apretar" de "mantener" OK en un control remoto. [TvClickable.kt](app/src/main/java/com/consola/launcher/ui/TvClickable.kt) lo resuelve con la misma regla que usa Android: mientras mantenés la tecla, el sistema la repite, y al cumplirse su tiempo de "mantener" marca la repetición con `FLAG_LONG_PRESS`. Al principio usé un umbral propio más largo (600 ms) y con un control que repite una sola vez quedaba como "toque" y abría la app. Usar la marca del sistema lo hace coincidir con el resto de la tele.

## Un detalle de foco que vale la pena entender

Al subir desde una app hacia las pestañas, la búsqueda de foco 2D de Compose elige la pestaña **más cercana en línea recta**: desde Spotify caía en Ajustes y te cambiaba de pestaña sin querer. `focusRestorer()` no lo evitó (medido en la tele). La solución en [HomeScreen.kt](app/src/main/java/com/consola/launcher/home/HomeScreen.kt) es determinística: mientras el foco está en el contenido, las pestañas no son enfocables. ARRIBA primero intenta moverse dentro del contenido, y si ya no hay nada más arriba, va a la pestaña donde estabas.
