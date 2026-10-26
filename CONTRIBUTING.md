# Cómo contribuir

¡Gracias por querer sumar! Unas reglas cortas para que todo quede prolijo.

## Antes de empezar

- Para cambios grandes, abrí primero un **issue** contando la idea: así lo charlamos antes de que inviertas tiempo.
- Leé la sección "Antes de tocar código" del [README](README.md): el foco en la tele y los decodificadores de video condicionan casi todo.

## Compilar y probar

```bash
./gradlew testDebugUnitTest   # tests (JVM, no hace falta una tele)
./gradlew assembleDebug       # APK en app/build/outputs/apk/debug
```

Toda lógica nueva va con **tests primero**: lo que se pueda separar de Android (cálculos, formatos, reglas) vive en clases puras y se prueba sin emulador. Mirá cualquier `*Test.kt` de `app/src/test` como ejemplo.

## Estilo

- Kotlin idiomático y Jetpack Compose. Los comentarios explican el **por qué**, no el qué.
- Comentarios y textos de la interfaz en español rioplatense, como el resto del proyecto.
- Commits con [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/): `feat:`, `fix:`, `docs:`, `refactor:`, `test:`...

## Rendimiento

Este launcher corre en teles con poca memoria y GPUs modestas (Mali-G52). Si tu cambio dibuja algo que se mueve, medilo en una tele de verdad con `adb shell dumpsys gfxinfo com.consola.launcher` antes y después, y contá los números en el pull request.
