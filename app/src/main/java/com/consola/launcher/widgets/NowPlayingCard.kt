package com.consola.launcher.widgets

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.background.AccentTone
import com.consola.launcher.ui.MediaIcons
import kotlinx.coroutines.delay

enum class MediaAction { PlayPause, Next, Previous, OpenPlayer }

private val CardShape = RoundedCornerShape(20.dp)
private val NeutralCard = Color(0xFF1C2130)

/** "Reproduciendo ahora": la carátula, la canción, el avance y los controles, teñidos con el color del disco. */
@Composable
fun NowPlayingCard(state: NowPlayingState, onAction: (MediaAction) -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        is NowPlayingState.Active -> ActiveCard(state.track, onAction, modifier)
        is NowPlayingState.Idle -> if (state.suggestedLabel != null) IdleCard(state.suggestedLabel, onAction, modifier)
        // Sin acceso no mostramos nada en el Inicio: el aviso para habilitarlo vive en Ajustes → Widgets.
        NowPlayingState.NoAccess -> Unit
    }
}

/**
 * La versión compacta, para Minimal: una cápsula de una sola línea debajo del reloj.
 * La tarjeta grande arriba a la derecha se pisaba con el reloj centrado (sobre todo en formato 12 h).
 */
@Composable
fun NowPlayingPill(state: NowPlayingState, onAction: (MediaAction) -> Unit, modifier: Modifier = Modifier) {
    val track = (state as? NowPlayingState.Active)?.track ?: return
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
                .focusableAction { onAction(MediaAction.OpenPlayer) },
        ) {
            track.art?.let { Image(it, contentDescription = track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = listOfNotNull(track.title, track.artist).joinToString("  ·  "),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Spacer(Modifier.width(12.dp))
        ControlButton(if (track.isPlaying) MediaIcons.Pause else Icons.Default.PlayArrow, size = 40.dp, primary = true) {
            onAction(MediaAction.PlayPause)
        }
    }
}

@Composable
private fun ActiveCard(track: Track, onAction: (MediaAction) -> Unit, modifier: Modifier) {
    // El fondo de la tarjeta toma el color del disco, oscurecido para que el texto blanco se lea.
    val tint by animateColorAsState(
        targetValue = track.accentArgb?.let { Color(AccentTone.forBackground(it, maxBrightness = 0.35f)) } ?: NeutralCard,
        animationSpec = tween(600),
        label = "nowPlayingTint",
    )

    // UNA sola fila: carátula, texto y controles uno al lado del otro. Con dos "pisos" (carátula arriba,
    // controles abajo) había que apretar ARRIBA tres veces para salir de la fila de apps a las pestañas.
    Row(
        modifier
            .width(440.dp)
            .clip(CardShape)
            .background(tint.copy(alpha = 0.88f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .focusableAction { onAction(MediaAction.OpenPlayer) },
            ) {
                track.art?.let { Image(it, contentDescription = track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.appLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                )
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                track.artist?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                ProgressLine(track)
            }
            Spacer(Modifier.width(10.dp))
            ControlButton(MediaIcons.SkipPrevious, size = 34.dp) { onAction(MediaAction.Previous) }
            Spacer(Modifier.width(8.dp))
            ControlButton(if (track.isPlaying) MediaIcons.Pause else Icons.Default.PlayArrow, size = 44.dp, primary = true) {
                onAction(MediaAction.PlayPause)
            }
            Spacer(Modifier.width(8.dp))
            ControlButton(MediaIcons.SkipNext, size = 34.dp) { onAction(MediaAction.Next) }
    }
}

/** La barrita de avance. La app de música avisa la posición solo cuando cambia algo: el resto lo calculamos. */
@Composable
private fun ProgressLine(track: Track) {
    if (track.durationMs <= 0) return
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(track.isPlaying) {
        while (track.isPlaying) {
            now = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val elapsed = if (track.isPlaying) ((now - track.positionUpdatedAt) * track.speed).toLong() else 0L
    val fraction = ((track.positionMs + elapsed).toFloat() / track.durationMs).coerceIn(0f, 1f)
    val animated by animateFloatAsState(fraction, label = "progress")

    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.25f))) {
        Box(Modifier.fillMaxWidth(animated).height(4.dp).clip(RoundedCornerShape(50)).background(Color.White))
    }
}

@Composable
private fun IdleCard(playerLabel: String, onAction: (MediaAction) -> Unit, modifier: Modifier) {
    Row(
        modifier
            .clip(CardShape)
            .background(NeutralCard.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.now_playing_idle), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
        Spacer(Modifier.width(14.dp))
        PillButton(stringResource(R.string.now_playing_open, playerLabel)) { onAction(MediaAction.OpenPlayer) }
    }
}

@Composable
private fun ControlButton(icon: ImageVector, size: Dp, primary: Boolean = false, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, label = "controlScale")
    val background = when {
        isFocused -> Color.White
        primary -> Color.White.copy(alpha = 0.9f)
        else -> Color.White.copy(alpha = 0.15f)
    }
    val content = if (isFocused || primary) Color.Black else Color.White

    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(background)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (isFocused) Color.Black else Color.White,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(50))
            .background(if (isFocused) Color.White else Color(0xFF1DB954))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** La carátula también es un botón (abre la app): se marca con un borde al tener el foco. */
@Composable
private fun Modifier.focusableAction(onClick: () -> Unit): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    return this
        .then(if (isFocused) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier)
        .onFocusChanged { isFocused = it.isFocused }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
