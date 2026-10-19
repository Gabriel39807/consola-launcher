package com.consola.launcher.ui

import android.view.ViewConfiguration
import androidx.compose.foundation.focusable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/** Las teclas que significan "OK" según el control: el botón central del D-pad o Enter. */
val ConfirmKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

fun KeyEvent.isConfirm(): Boolean = key in ConfirmKeys

/**
 * Como clickable(), pero distingue APRETAR OK de MANTENER OK en el control remoto.
 *
 * Mientras la mantenés, Android repite el KeyDown, y al cumplirse su tiempo de "mantener" marca la
 * repetición con FLAG_LONG_PRESS (isLongPress). Usamos ESA marca y el mismo umbral del sistema: con un
 * umbral propio más largo, un control que repite una sola vez quedaba como "toque" y abría la app.
 * Si el control no repite la tecla, lo decidimos al soltarla. Un toque corto es [onClick].
 */
fun Modifier.tvClickable(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier = composed {
    val press = remember { PressState() }
    this
        .onKeyEvent { event ->
            if (!event.isConfirm()) return@onKeyEvent false
            val native = event.nativeKeyEvent
            val heldLongEnough = native.isLongPress || native.eventTime - native.downTime >= ViewConfiguration.getLongPressTimeout()
            when (event.type) {
                KeyEventType.KeyDown -> {
                    if (native.repeatCount == 0) press.longPressFired = false
                    if (onLongClick != null && !press.longPressFired && heldLongEnough) {
                        press.longPressFired = true
                        onLongClick()
                    }
                    true
                }
                KeyEventType.KeyUp -> {
                    when {
                        press.longPressFired -> Unit
                        onLongClick != null && heldLongEnough -> onLongClick()
                        else -> onClick()
                    }
                    press.longPressFired = false
                    true
                }
                else -> false
            }
        }
        .focusable()
}

private class PressState {
    var longPressFired = false
}
