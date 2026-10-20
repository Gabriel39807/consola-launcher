package com.consola.launcher.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.ui.isConfirm

/**
 * El "mantener OK y mover" de las favoritas, igual en los cuatro estilos de Inicio.
 * Cada estilo acomoda las apps a su manera (fila, grilla, íconos chicos), pero el ORDEN es una lista:
 * ◀ ▶ mueven dentro de esa lista. Por eso la lógica vive acá y no en cada estilo.
 */
@Stable
class ReorderState {
    /** Copia local del orden: al mover, la app cambia de lugar AL INSTANTE, sin esperar a que se guarde. */
    var order by mutableStateOf<List<HomeTile>>(emptyList())
        private set
    var movingKey by mutableStateOf<String?>(null)
        private set
    /** Adónde fue la última app movida: cada estilo lo usa para seguirla con su propio scroll. */
    var movedToIndex by mutableStateOf<Int?>(null)
        private set

    internal var onReorder: (List<String>) -> Unit = {}
    // Al soltar con OK, el KeyUp de ese OK no tiene que abrir la app.
    private var swallowNextConfirmUp = false

    internal fun sync(tiles: List<HomeTile>) {
        order = tiles
    }

    fun isMoving(tile: HomeTile): Boolean = tile.key == movingKey

    fun startMoving(tile: HomeTile) {
        movingKey = tile.key
    }

    /** Para el onPreviewKeyEvent del contenedor de cada estilo. Devuelve true si se quedó con la tecla. */
    fun handleKey(event: KeyEvent): Boolean {
        if (movingKey == null) {
            if (swallowNextConfirmUp && event.type == KeyEventType.KeyUp && event.isConfirm()) {
                swallowNextConfirmUp = false
                return true
            }
            return false
        }
        // Mientras movés, el contenedor se queda con TODAS las teclas: ◀ ▶ mueven, OK o Atrás sueltan,
        // y arriba/abajo no hacen nada (así no te salís con una app "en el aire").
        if (event.type == KeyEventType.KeyDown) {
            val isFreshPress = event.nativeKeyEvent.repeatCount == 0
            when {
                event.key == Key.DirectionLeft -> moveBy(-1)
                event.key == Key.DirectionRight -> moveBy(+1)
                // Las repeticiones del OK que mantuviste para empezar a mover NO sueltan la app.
                event.isConfirm() && isFreshPress -> {
                    movingKey = null
                    swallowNextConfirmUp = true
                }
                event.key == Key.Back -> movingKey = null
            }
        }
        return true
    }

    private fun moveBy(delta: Int) {
        val from = order.indexOfFirst { it.key == movingKey }
        val to = from + delta
        if (from < 0 || to !in order.indices) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        movedToIndex = to
        onReorder(order.map { it.packageName })
    }
}

@Composable
fun rememberReorderState(tiles: List<HomeTile>, onReorder: (List<String>) -> Unit): ReorderState {
    val state = remember { ReorderState() }
    val currentOnReorder by rememberUpdatedState(onReorder)
    state.onReorder = { currentOnReorder(it) }
    LaunchedEffect(tiles) { state.sync(tiles) }
    return state
}

/** Mantener OK: si es favorita, la movés; si no (Inicio sin favoritas), la agregás. */
fun ReorderState.longPress(tile: HomeTile, onToggleFavorite: (HomeTile) -> Unit) {
    if (tile.isFavorite) startMoving(tile) else onToggleFavorite(tile)
}

/**
 * El foco va a la primera app UNA sola vez, al abrir el launcher. Si lo hiciéramos cada vez que se
 * muestra Inicio, el foco saltaría solo desde las pestañas mientras navegás.
 */
@Composable
fun InitialFocus(requester: FocusRequester, enabled: Boolean, ready: Boolean, onDone: () -> Unit) {
    LaunchedEffect(enabled, ready) {
        if (enabled && ready) {
            withFrameNanos { } // los ítems de una lista "lazy" existen recién después del primer layout
            requester.requestFocus()
            onDone()
        }
    }
}

/** La única ayuda en pantalla, y solo mientras movés: es cuando de verdad hace falta. */
@Composable
fun MoveHint(visible: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "moveHint")
    Text(
        text = stringResource(R.string.move_hint),
        style = MaterialTheme.typography.titleSmall,
        color = Color.White,
        modifier = modifier.graphicsLayer { this.alpha = alpha },
    )
}
