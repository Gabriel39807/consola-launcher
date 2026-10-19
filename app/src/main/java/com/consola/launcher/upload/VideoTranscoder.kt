package com.consola.launcher.upload

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Convierte videos al formato que esta tele reproduce sin esfuerzo, usando su codificador por hardware. */
@OptIn(UnstableApi::class)
class VideoTranscoder(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Códec y tamaño de la pista de video, o null si no es un video que la tele pueda leer. Es I/O. */
    fun probe(file: File): VideoInfo? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            (0 until extractor.trackCount)
                .map(extractor::getTrackFormat)
                .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
                ?.let { format ->
                    VideoInfo(
                        mime = format.getString(MediaFormat.KEY_MIME).orEmpty(),
                        width = format.getInteger(MediaFormat.KEY_WIDTH),
                        height = format.getInteger(MediaFormat.KEY_HEIGHT),
                    )
                }
        } catch (e: IOException) {
            null
        } finally {
            extractor.release()
        }
    }

    /** A H.264, como máximo 1080 de alto y sin audio (es un fondo: el audio solo gasta). */
    suspend fun toTvFormat(input: File, output: File, onProgress: (Int) -> Unit) = withContext(Dispatchers.Main) {
        // Transformer exige que lo usen desde un hilo con Looper: por eso todo esto corre en el principal.
        suspendCancellableCoroutine { continuation ->
            val progress = ProgressHolder()
            lateinit var poll: Runnable

            val transformer = Transformer.Builder(appContext)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        mainHandler.removeCallbacks(poll)
                        continuation.resume(Unit)
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        mainHandler.removeCallbacks(poll)
                        continuation.resumeWithException(exportException)
                    }
                })
                .build()

            // Transformer no avisa el progreso: hay que preguntarle.
            poll = Runnable {
                if (transformer.getProgress(progress) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(progress.progress)
                mainHandler.postDelayed(poll, PROGRESS_POLL_MILLIS)
            }

            val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(input)))
                .setRemoveAudio(true)
                .setEffects(Effects(emptyList(), listOf(Presentation.createForHeight(VideoOptimizer.MAX_HEIGHT))))
                .build()
            transformer.start(item, output.absolutePath)
            mainHandler.post(poll)

            continuation.invokeOnCancellation {
                mainHandler.post {
                    mainHandler.removeCallbacks(poll)
                    transformer.cancel()
                }
            }
        }
    }

    private companion object {
        const val PROGRESS_POLL_MILLIS = 500L
    }
}
