package com.consola.launcher.dream

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.LauncherGraph
import com.consola.launcher.background.AnimatedBackground
import com.consola.launcher.background.BackgroundCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DreamAccent = Color(0xFF2E4BFF)

// La hora cambia de esquina cada minuto: un texto quieto horas y horas puede quedar "marcado" en la pantalla.
private val ClockCorners = listOf(Alignment.BottomEnd, Alignment.BottomStart, Alignment.TopStart, Alignment.TopEnd)

private val ReadableShadow = Shadow(color = Color.Black.copy(alpha = 0.7f), offset = Offset(0f, 2f), blurRadius = 16f)

@Composable
fun DreamScreen(graph: LauncherGraph) {
    val settings by graph.settings.settings.collectAsState(initial = null)
    val videos by graph.backgrounds.videos.collectAsState()
    var libraryScanned by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { graph.backgrounds.rescan() }
        libraryScanned = true
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val current = settings
        // Esperamos a leer la carpeta: si no, se vería un instante la aurora antes de tu video.
        if (current != null && libraryScanned) {
            val video = BackgroundCatalog.resolve(current.background, videos)?.let(graph.backgrounds::uriOf)
            AnimatedBackground(video = video, accent = DreamAccent, withScrim = false)
        }
        DreamClock()
    }
}

@Composable
private fun DreamClock() {
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEEE d MMMM") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var corner by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            val previousMinute = now.minute
            now = LocalDateTime.now()
            if (now.minute != previousMinute) corner = (corner + 1) % ClockCorners.size
        }
    }

    AnimatedContent(
        targetState = corner,
        transitionSpec = { fadeIn(tween(1_000)) togetherWith fadeOut(tween(1_000)) },
        label = "dreamClock",
        modifier = Modifier.fillMaxSize(),
    ) { index ->
        val alignment = ClockCorners[index]
        Box(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 48.dp)) {
            Column(
                modifier = Modifier.align(alignment),
                horizontalAlignment = if (alignment == Alignment.BottomEnd || alignment == Alignment.TopEnd) Alignment.End else Alignment.Start,
            ) {
                Text(
                    text = now.format(timeFormat),
                    style = MaterialTheme.typography.displayLarge.copy(shadow = ReadableShadow),
                    fontWeight = FontWeight.Light,
                )
                Text(
                    text = now.format(dayFormat).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge.copy(shadow = ReadableShadow),
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}
