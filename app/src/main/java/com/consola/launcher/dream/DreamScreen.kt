package com.consola.launcher.dream

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.LauncherGraph
import com.consola.launcher.background.AnimatedBackground
import com.consola.launcher.background.BackgroundCatalog
import com.consola.launcher.settings.ClockStyle
import com.consola.launcher.ui.ClockPlacement
import com.consola.launcher.ui.LiveClock
import com.consola.launcher.widgets.NowPlayingState
import com.consola.launcher.widgets.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalTime

private val DreamAccent = Color(0xFF2E4BFF)

// La hora cambia de esquina cada minuto: un texto quieto horas y horas puede quedar "marcado" en la pantalla.
private val ClockCorners = listOf(Alignment.BottomEnd, Alignment.BottomStart, Alignment.TopStart, Alignment.TopEnd)

private val ReadableShadow = Shadow(color = Color.Black.copy(alpha = 0.7f), offset = Offset(0f, 2f), blurRadius = 16f)

/** El protector: tu fondo (con sus cambios automáticos y su pulso), la hora, tu mensaje y lo que suena. */
@Composable
fun DreamScreen(graph: LauncherGraph) {
    val settings by graph.settings.settings.collectAsState(initial = null)
    val videos by graph.backgrounds.videos.collectAsState()
    val nowPlaying by graph.nowPlaying.state.collectAsState()
    val audioLevels = graph.audioPulse.levels.collectAsState()
    var libraryScanned by remember { mutableStateOf(false) }
    var corner by remember { mutableIntStateOf(0) }
    var epochMinute by remember { mutableLongStateOf(System.currentTimeMillis() / 60_000) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { graph.backgrounds.rescan() }
        libraryScanned = true
    }
    // Un solo reloj interno: cada minuto mueve la hora de esquina y deja que el fondo cambie si toca.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            epochMinute = System.currentTimeMillis() / 60_000
            corner = (corner + 1) % ClockCorners.size
        }
    }
    // La música del protector: se escucha solo mientras el protector está a la vista.
    LifecycleStartEffect(Unit) {
        graph.nowPlaying.start()
        onStopOrDispose { graph.nowPlaying.stop() }
    }
    val musicReactive = settings?.tuning?.musicReactive == true
    LifecycleStartEffect(musicReactive) {
        if (musicReactive) graph.audioPulse.start()
        onStopOrDispose { if (musicReactive) graph.audioPulse.stop() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val current = settings
        // Esperamos a leer la carpeta: si no, se vería un instante la aurora antes de tu video.
        if (current != null && libraryScanned) {
            val video = BackgroundCatalog.resolveTimed(current.background, videos, current.tuning, LocalTime.now(), epochMinute)
                ?.let(graph.backgrounds::uriOf)
            AnimatedBackground(
                video = video,
                accent = DreamAccent,
                withScrim = false,
                pulse = audioLevels.takeIf { musicReactive },
            )
        }
        DreamOverlay(
            corner = corner,
            style = current?.clock ?: ClockStyle(),
            message = current?.dreamMessage.orEmpty(),
            track = (nowPlaying as? NowPlayingState.Active)?.track,
        )
    }
}

@Composable
private fun DreamOverlay(corner: Int, style: ClockStyle, message: String, track: Track?) {
    AnimatedContent(
        targetState = corner,
        transitionSpec = { fadeIn(tween(1_000)) togetherWith fadeOut(tween(1_000)) },
        label = "dreamOverlay",
        modifier = Modifier.fillMaxSize(),
    ) { index ->
        val alignment = ClockCorners[index]
        val toTheRight = alignment == Alignment.BottomEnd || alignment == Alignment.TopEnd
        val horizontal = if (toTheRight) Alignment.End else Alignment.Start
        Box(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 48.dp)) {
            Column(Modifier.align(alignment), horizontalAlignment = horizontal) {
                // El mismo reloj que el Inicio: lo que elegís en Ajustes → Reloj también vale acá.
                LiveClock(style = style, placement = ClockPlacement.Hero, shadow = ReadableShadow, horizontalAlignment = horizontal)
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.headlineSmall.copy(shadow = ReadableShadow),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 640.dp),
                    )
                }
                track?.let {
                    Spacer(Modifier.height(16.dp))
                    DreamTrack(it)
                }
            }
        }
    }
}

/** Lo que suena, en una línea: el protector no tiene botones, así que solo informa. */
@Composable
private fun DreamTrack(track: Track) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.3f))) {
            track.art?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = listOfNotNull(track.title, track.artist).joinToString("  ·  "),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 420.dp).padding(end = 12.dp),
        )
    }
}
