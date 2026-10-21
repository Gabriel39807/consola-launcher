package com.consola.launcher.keys

import android.view.KeyEvent

/** Qué teclas del control son "el botón Ajustes". Puro: se testea sin Android. */
object RemoteKeys {
    // El estándar es SETTINGS, pero cada fabricante mapea su control: en las KTC (MediaTek) el botón de
    // ajustes llega como FOCUS (scan 240). Medido con el control real: las teclas por adb no sirven para esto.
    private val SETTINGS_KEYS = setOf(KeyEvent.KEYCODE_SETTINGS, KeyEvent.KEYCODE_FOCUS)

    fun isSettingsKey(keyCode: Int): Boolean = keyCode in SETTINGS_KEYS

    // Botones que la tele le mandaba SOLO al inicio de Google (dumpsys window policy → teclas globales de launcherx).
    // Con Google apagado no hacían nada: ahora los atiende Consola.
    private val SHORTCUTS = mapOf(
        KeyEvent.KEYCODE_ALL_APPS to RemoteShortcut.AllApps,
        KeyEvent.KEYCODE_GUIDE to RemoteShortcut.LiveTv,
        KeyEvent.KEYCODE_NOTIFICATION to RemoteShortcut.QuickPanel,
    )

    fun shortcutFor(keyCode: Int): RemoteShortcut? = SHORTCUTS[keyCode]
}

/** Qué hace Consola con un botón huérfano. Se ejecuta al soltarlo. */
enum class RemoteShortcut { AllApps, LiveTv, QuickPanel }

enum class ButtonAction { QuickPanel, FullSettings }

/**
 * Un toque abre el panel de ajustes rápidos; mantenerlo [holdMs] abre los ajustes completos.
 *
 * Los ajustes completos se abren AL CUMPLIRSE el tiempo (con [onHoldCheck], que llama un temporizador), no al
 * soltar: así se siente que respondió. Los tiempos son los del evento de teclado, en milisegundos.
 */
class SettingsButton(private val holdMs: Long = HOLD_MS) {
    private var downAt = NOT_PRESSED
    private var fired = false

    /** Devuelve true si es un toque NUEVO: ahí hay que programar el chequeo de los [holdMs]. */
    fun onDown(now: Long, repeatCount: Int): Boolean {
        // Mientras se mantiene, el control repite la tecla: eso no es un toque nuevo.
        if (repeatCount > 0) return false
        // Si se perdió un "soltar", este toque igual empieza de cero.
        downAt = now
        fired = false
        return true
    }

    fun onHoldCheck(now: Long): ButtonAction? {
        if (downAt == NOT_PRESSED || fired || now - downAt < holdMs) return null
        fired = true
        return ButtonAction.FullSettings
    }

    fun onUp(now: Long): ButtonAction? {
        if (downAt == NOT_PRESSED) return null
        val held = now - downAt
        val alreadyOpened = fired
        downAt = NOT_PRESSED
        fired = false
        return when {
            alreadyOpened -> null
            // Por si el temporizador se atrasó: lo que importa es cuánto lo mantuvo.
            held >= holdMs -> ButtonAction.FullSettings
            else -> ButtonAction.QuickPanel
        }
    }

    companion object {
        const val HOLD_MS = 3_000L
        private const val NOT_PRESSED = -1L
    }
}
