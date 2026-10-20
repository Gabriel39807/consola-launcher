package com.consola.launcher.background

import com.consola.launcher.settings.BackgroundChoice
import com.consola.launcher.settings.BackgroundTuning
import com.consola.launcher.settings.Rotation
import java.time.LocalTime

object BackgroundCatalog {

    private val DAY_STARTS = LocalTime.of(7, 0)
    private val NIGHT_STARTS = LocalTime.of(19, 0)

    /**
     * Qué video reproducir según lo elegido y lo que hay en la carpeta, o null para la aurora.
     * [videos] viene del más nuevo al más viejo: "automático" = lo último que subiste.
     */
    fun resolve(choice: BackgroundChoice?, videos: List<String>): String? = when (choice) {
        BackgroundChoice.Aurora -> null
        // Si el video elegido se borró desde el celular, no dejamos la pantalla negra: caemos al automático.
        is BackgroundChoice.Video -> choice.fileName.takeIf { it in videos } ?: videos.firstOrNull()
        null -> videos.firstOrNull()
    }

    /**
     * Como [resolve], pero con los fondos que cambian solos. Puro: la hora llega de afuera, así se testea
     * sin esperar media hora. [epochMinute] = minutos desde 1970: todas las pantallas (Inicio y protector)
     * calculan lo mismo en el mismo momento.
     */
    fun resolveTimed(
        choice: BackgroundChoice?,
        videos: List<String>,
        tuning: BackgroundTuning,
        time: LocalTime,
        epochMinute: Long,
    ): String? = when (tuning.rotation) {
        Rotation.Off -> resolve(choice, videos)
        Rotation.Every15 -> rotate(videos, epochMinute, period = 15)
        Rotation.Every30 -> rotate(videos, epochMinute, period = 30)
        Rotation.Every60 -> rotate(videos, epochMinute, period = 60)
        Rotation.DayNight -> {
            val isDay = !time.isBefore(DAY_STARTS) && time.isBefore(NIGHT_STARTS)
            // Sin un fondo marcado para este momento (o si se borró), la aurora: nunca un video "al azar".
            (if (isDay) tuning.dayVideo else tuning.nightVideo)?.takeIf { it in videos }
        }
    }

    private fun rotate(videos: List<String>, epochMinute: Long, period: Long): String? {
        if (videos.isEmpty()) return null
        return videos[((epochMinute / period) % videos.size).toInt()]
    }
}
