package com.consola.launcher.upload

import com.consola.launcher.widgets.Countdown
/**
 * Reglas para lo que llega desde el celular. El nombre del archivo lo manda OTRO dispositivo:
 * se trata como entrada no confiable, igual que un formulario web.
 */
object UploadRules {

    const val MAX_BASE_NAME_LENGTH = 60
    const val MAX_UPLOAD_BYTES = 500L * 1024 * 1024

    val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "mov", "webm", "mkv")
    private val UNSAFE_CHARS = Regex("[^a-z0-9._-]+")

    /** Un nombre seguro y que no pisa otro fondo, o null si no es un video. */
    fun safeFileName(original: String, existing: Set<String>): String? {
        // Solo la última parte: "../../etc/passwd.mp4" no puede escaparse de la carpeta de fondos.
        val leaf = original.substringAfterLast('/').substringAfterLast('\\')
        val extension = leaf.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (extension !in VIDEO_EXTENSIONS) return null

        val base = leaf.substringBeforeLast('.')
            .lowercase()
            .replace(UNSAFE_CHARS, "-")
            .trim('-', '.', '_')
            .take(MAX_BASE_NAME_LENGTH)
            .ifEmpty { "fondo" }

        var candidate = "$base.$extension"
        var copy = 2
        while (candidate in existing) {
            candidate = "$base-$copy.$extension"
            copy++
        }
        return candidate
    }

    const val MAX_NAME_LENGTH = 40
    const val MAX_MESSAGE_LENGTH = 120
    const val MAX_COUNTDOWN_LENGTH = 30
    const val MAX_CITY_LENGTH = 60
    const val MAX_COVER_BYTES = 10L * 1024 * 1024

    private val CONTROL_CHARS = Regex("\\p{Cntrl}+")
    private val SPACES = Regex(" {2,}")

    /** Nombres y mensajes que llegan del celular: sin caracteres de control, sin espacios de más y con un largo máximo. */
    fun cleanText(raw: String, maxLength: Int): String =
        raw.replace(CONTROL_CHARS, " ").replace(SPACES, " ").trim().take(maxLength).trim()

    /** La cuenta regresiva que llega del celular. Sin título = borrarla; la fecha viene del selector (aaaa-mm-dd). */
    fun countdownInput(title: String, date: String): CountdownInput {
        val clean = cleanText(title, MAX_COUNTDOWN_LENGTH)
        if (clean.isEmpty()) return CountdownInput.Clear
        val countdown = Countdown.decode("$clean\t${date.trim()}") ?: return CountdownInput.Invalid
        return CountdownInput.Valid(countdown)
    }
}

sealed interface CountdownInput {
    data object Clear : CountdownInput
    data object Invalid : CountdownInput
    data class Valid(val countdown: Countdown) : CountdownInput
}
