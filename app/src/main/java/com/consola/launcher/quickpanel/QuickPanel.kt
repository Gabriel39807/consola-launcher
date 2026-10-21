package com.consola.launcher.quickpanel

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.settings.ClockStyle
import com.consola.launcher.ui.ClockPlacement
import com.consola.launcher.ui.LiveClock
import com.consola.launcher.ui.MediaIcons
import com.consola.launcher.widgets.MediaAction
import com.consola.launcher.widgets.NowPlayingSource
import com.consola.launcher.widgets.NowPlayingState
import com.consola.launcher.widgets.Track
import kotlinx.coroutines.delay

// Opaco: con transparencia se colaba lo de atrás (la hora, la música) y costaba leer.
private val PanelColor = Color(0xFF0E1220)
private val PanelShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
private val ChipShape = RoundedCornerShape(14.dp)
private val ChipColor = Color.White.copy(alpha = 0.07f)

// Angosto como el de Google TV (~30 % de la pantalla): se ve lo que estás mirando.
private val PanelWidth = 300.dp
private const val SLIDE_MS = 200
private const val FOCUS_MS = 120
private val ConnectedColor = Color(0xFF34C759)

/**
 * El panel lateral: lo que suena, el volumen, las entradas de la tele y los atajos. Entra y sale deslizándose.
 *
 * Está SIEMPRE armado (la capa lo esconde): abrirlo es solo animar [open]. Escondido no escucha la música, no
 * mira el volumen ni hace andar el reloj: todo eso se prende con [open]. Al terminar de salir avisa [onHidden].
 */
@Composable
internal fun QuickPanel(
    open: Boolean,
    openCount: Int,
    actions: List<QuickAction>,
    inputs: List<InputEntry>,
    clock: ClockStyle?,
    nowPlaying: NowPlayingSource,
    onAction: (QuickAction) -> Unit,
    onInput: (InputEntry) -> Unit,
    onCloseRequest: () -> Unit,
    onHidden: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(SLIDE_MS, easing = FastOutSlowInEasing),
        label = "panel",
        finishedListener = { if (it == 0f) onHidden() },
    )
    val visible = open || progress > 0f
    val scroll = rememberScrollState()
    val firstFocus = remember { FocusRequester() }

    // La música se escucha solo mientras el panel está abierto.
    if (open) {
        DisposableEffect(nowPlaying) {
            nowPlaying.start()
            onDispose { nowPlaying.stop() }
        }
    }
    val playing by nowPlaying.state.collectAsState()

    // Cada apertura: la lista arriba de todo y el foco en lo primero (la primera entrada).
    LaunchedEffect(openCount) {
        if (!open) return@LaunchedEffect
        scroll.scrollTo(0)
        withFrameNanos { }
        runCatching { firstFocus.requestFocus() }
    }

    val shortcuts = if (inputs.isEmpty()) actions else actions.filter { it.id != QuickActionId.Inputs }

    Box(
        Modifier
            .fillMaxSize()
            // El panel es una capa, no una pantalla: Back lo manejamos a mano (no hay BackHandler fuera de una Activity).
            .onPreviewKeyEvent { event ->
                if (event.key != Key.Back && event.key != Key.Escape) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyUp) onCloseRequest()
                true
            }
            // Oscurece apenas lo de atrás, cerca del panel. Acompaña al panel: aparece y se va con él.
            .background(Brush.horizontalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f * progress))),
    ) {
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .width(PanelWidth)
                .fillMaxHeight()
                // Deslizar con graphicsLayer solo mueve la capa ya dibujada: no se vuelve a dibujar el panel.
                .graphicsLayer { translationX = (1f - progress) * size.width }
                .clip(PanelShape)
                .background(PanelColor)
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    stringResource(R.string.quick_panel_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(top = 2.dp),
                )
                // El reloj avanza solo con el panel a la vista.
                if (visible) clock?.let { LiveClock(style = it, placement = ClockPlacement.Bar) }
            }
            Spacer(Modifier.height(10.dp))
            (playing as? NowPlayingState.Active)?.takeIf { open }?.let { active ->
                NowPlayingRow(active.track, onAction = nowPlaying::perform)
                Spacer(Modifier.height(8.dp))
            }
            VolumeSlider(active = open)
            if (inputs.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                SectionLabel(stringResource(R.string.quick_inputs))
                InputsRow(inputs, onInput, firstFocus)
            }
            Spacer(Modifier.height(10.dp))
            // 2 columnas fijas, como chips. "Personalizar" queda al lado de "Más ajustes".
            shortcuts.chunked(2).forEachIndexed { row, pair ->
                if (row > 0) Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pair.forEachIndexed { column, action ->
                        val first = inputs.isEmpty() && row == 0 && column == 0
                        QuickChip(
                            action = action,
                            onClick = { onAction(action) },
                            modifier = Modifier
                                .weight(1f)
                                .then(if (first) Modifier.focusRequester(firstFocus) else Modifier),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.quick_panel_hint),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.5.sp,
        color = Color.White.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )
}

/** Las entradas de la tele en una fila: un clic y estás en el HDMI. El punto verde = hay algo prendido ahí. */
@Composable
private fun InputsRow(inputs: List<InputEntry>, onInput: (InputEntry) -> Unit, firstFocus: FocusRequester) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        inputs.forEachIndexed { index, input ->
            var focused by remember { mutableStateOf(false) }
            val (scale, background, content) = focusLook(focused)
            Row(
                (if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                    .height(36.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(ChipShape)
                    .background(background)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onInput(input) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (input.kind == InputKind.Tuner) PanelIcons.Antenna else PanelIcons.Input,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(input.label, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
                if (input.connected) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(ConnectedColor))
                }
            }
        }
    }
}

@Composable
private fun QuickChip(action: QuickAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val (scale, background, content) = focusLook(focused)
    Row(
        modifier
            .height(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(ChipShape)
            .background(background)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PanelIcons.forAction(action.id), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(action.label),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * El volumen como un deslizador: con el foco encima, IZQUIERDA/DERECHA lo bajan/suben y OK silencia.
 * Se mantiene al día si lo cambiás con los botones del control con el panel abierto.
 * Si la tele tiene el volumen fijo (por ejemplo, lo maneja una barra de sonido por HDMI), no se muestra.
 */
@Composable
private fun VolumeSlider(active: Boolean) {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    if (audio.isVolumeFixed) return
    val max = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var level by remember { mutableIntStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var muted by remember { mutableStateOf(audio.isStreamMute(AudioManager.STREAM_MUSIC)) }
    var focused by remember { mutableStateOf(false) }

    fun sync() {
        level = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        muted = audio.isStreamMute(AudioManager.STREAM_MUSIC)
    }
    // Android no avisa los cambios de volumen a las apps: con el panel abierto, miramos dos veces por segundo.
    LaunchedEffect(active) {
        while (active) {
            sync()
            delay(500)
        }
    }

    fun change(up: Boolean) {
        if (muted) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, VolumeStep.next(level, max, up), 0)
        sync()
    }

    fun toggleMute() {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)
        sync()
    }

    val fraction by animateFloatAsState(if (muted) 0f else level.toFloat() / max, label = "volume")
    val (_, background, content) = focusLook(focused)
    Row(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(ChipShape)
            .background(background)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { change(up = false); true }
                    Key.DirectionRight -> { change(up = true); true }
                    else -> false
                }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::toggleMute)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (muted) PanelIcons.VolumeOff else PanelIcons.VolumeUp,
            contentDescription = stringResource(if (muted) R.string.quick_volume_muted else R.string.quick_volume),
            tint = content,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(content.copy(alpha = 0.2f))) {
            Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(50)).background(content))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (muted) "—" else "${(level * 100f / max).toInt()}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = content,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End,
        )
    }
}

/** Cómo se ve algo con el foco: el cambio de color se anima (corto) en vez de saltar. */
@Composable
private fun focusLook(focused: Boolean): Triple<Float, Color, Color> {
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, tween(FOCUS_MS), label = "focusScale")
    val background by animateColorAsState(if (focused) Color.White else ChipColor, tween(FOCUS_MS), label = "focusBackground")
    val content by animateColorAsState(if (focused) Color.Black else Color.White, tween(FOCUS_MS), label = "focusContent")
    return Triple(scale, background, content)
}

/**
 * Lo que está sonando, con los mismos gestos que el volumen: OK pausa o sigue, IZQUIERDA/DERECHA cambian
 * de canción. Lleva el color del reproductor (Spotify, YouTube Music) para reconocerlo de un vistazo.
 */
@Composable
private fun NowPlayingRow(track: Track, onAction: (MediaAction) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val (_, background, content) = focusLook(focused)
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(ChipShape)
            .background(background)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onAction(MediaAction.Previous); true }
                    Key.DirectionRight -> { onAction(MediaAction.Next); true }
                    else -> false
                }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAction(MediaAction.PlayPause) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(Color.Black.copy(alpha = 0.3f))) {
            track.art?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                track.brand?.let { brand ->
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color(brand.argb)))
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    track.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            track.artist?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            if (track.isPlaying) MediaIcons.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(22.dp),
        )
    }
}
