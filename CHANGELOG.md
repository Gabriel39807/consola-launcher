# Cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versiones con [SemVer](https://semver.org/lang/es/).

## [0.7.0] - 2026-10-25

### Agregado
- **Visualizador de música** con cuatro estilos: Barras, Onda, Espejo y Fondo que late. Se dibuja abajo a lo ancho (detrás de las apps) o dentro de la tarjeta de la música, y opcionalmente en el protector de pantalla.
- **Widget de clima** (Open-Meteo, sin cuenta): ciudad automática o elegida desde el celular, °C o °F.
- **Cuenta regresiva** y **calendario del mes**.
- Pestaña **Widgets** en la página del celular (ciudad del clima y cuenta regresiva).

### Cambiado
- Los widgets se reubicaron en los 5 estilos de Inicio para que nada se pise en 960 × 540 dp.
- "Que el fondo baile" pasó de Fondos a un estilo del visualizador (se migra solo).

### Rendimiento
- El visualizador se dibuja en su propia capa, a 30 cuadros por segundo y con compuerta de ruido: el Inicio con música bajó de 86 % a 64 % de CPU en la tele de prueba.
- El audio se analiza solo en la pestaña Inicio.

## [0.6.0] - 2026-10-20

### Agregado
- **Ajustes rápidos propios**: un toque al botón Ajustes abre un panel lateral instantáneo (capa precargada) con lo que suena, volumen, entradas HDMI directas y atajos; mantenerlo 3 s abre los ajustes de la tele.
- Consola atiende los botones que solo manejaba el inicio de Google: Todas las apps, Guía y Notificaciones.
- Widget de música con **YouTube Music**, color por reproductor y reproductor preferido.

### Cambiado
- Home siempre vuelve a la pestaña Inicio; en Ajustes, ABAJO y IZQUIERDA llevan al menú de secciones.

## [0.5.1] - 2026-10-19

### Corregido
- `debloat.ps1` detecta y desactiva el asistente de configuración cuando le gana el botón Home a Consola.

## [0.5.0] - 2026-10-19

### Agregado
- Cinco estilos de Inicio: Cine, PS5, Xbox, Minimal y Filas.
- Reloj personalizable, temas (incluidos y propios) y cuatro letras.
- Fondos vivos: rotación, día/noche y oscurecido. Fondo que late con la música.
- Nombre y portada propios para cada app desde el celular.

## [0.3.0] - 2026-10-19

### Agregado
- Mantener OK para reordenar favoritas y agregarlas desde Apps.
- Protector de pantalla con tu fondo y la hora.

## [0.2.0] - 2026-10-19

### Agregado
- Pestañas Buscar, Inicio, Apps y Ajustes; búsqueda por voz.
- Subir fondos desde el celular con un QR.

## [0.1.0] - 2026-10-19

### Agregado
- Launcher para Google TV con estilo de consola y fondos de video animados.

[0.7.0]: https://github.com/Gabriel39807/consola-launcher/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/Gabriel39807/consola-launcher/compare/v0.5.1...v0.6.0
[0.5.1]: https://github.com/Gabriel39807/consola-launcher/compare/v0.5.0...v0.5.1
[0.5.0]: https://github.com/Gabriel39807/consola-launcher/compare/v0.3.0...v0.5.0
[0.3.0]: https://github.com/Gabriel39807/consola-launcher/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/Gabriel39807/consola-launcher/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Gabriel39807/consola-launcher/releases/tag/v0.1.0
