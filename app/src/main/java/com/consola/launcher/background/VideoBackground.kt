package com.consola.launcher.background

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.consola.launcher.R

@Composable
fun VideoBackground(video: Uri, onError: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentOnError by rememberUpdatedState(onError)
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    // El player vive SOLO mientras el inicio está visible. Una tele barata tiene pocos decodificadores
    // de video: si al abrir Netflix seguimos agarrando uno, Netflix puede quedarse sin él.
    LifecycleStartEffect(video) {
        val exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) = currentOnError()
            })
            prepare()
        }
        player = exoPlayer

        onStopOrDispose {
            player = null
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        // La configuración vive en el XML (TextureView, sin controles, zoom, NO enfocable).
        // Clave en TV: una View enfocable le roba el D-pad a Compose y los tiles dejan de responder.
        factory = { viewContext ->
            LayoutInflater.from(viewContext).inflate(R.layout.background_player, null) as PlayerView
        },
        update = { view -> view.player = player },
    )
}
