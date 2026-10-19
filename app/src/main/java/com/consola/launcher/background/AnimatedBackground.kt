package com.consola.launcher.background

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Video si hay uno que se pueda reproducir; si no, la aurora teñida con [accent]. Nunca una pantalla negra.
 * El degradé oscuro para leer el texto viene incluido en los dos casos (el protector de pantalla lo apaga).
 */
@Composable
fun AnimatedBackground(video: Uri?, accent: Color, modifier: Modifier = Modifier, withScrim: Boolean = true) {
    var videoFailed by remember(video) { mutableStateOf(false) }

    if (video != null && !videoFailed) {
        Box(modifier.fillMaxSize()) {
            VideoBackground(video = video, onError = { videoFailed = true })
            // Sobre el video no podemos hornear el degradé: va como capa aparte.
            if (withScrim) CinemaScrim()
        }
    } else {
        AuroraBackground(accent = accent, modifier = modifier, withScrim = withScrim)
    }
}
