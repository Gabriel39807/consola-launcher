package com.consola.launcher.upload

data class VideoInfo(
    val mime: String,
    val width: Int,
    val height: Int,
)

/**
 * ¿Hay que convertir este video antes de usarlo de fondo?
 *
 * Medido en una Challenger (Mali-G52): un 4K VP9 de YouTube como fondo deja la GPU en ~32 ms por frame
 * y 65 % de frames trabados; el mismo video en 1080p H.264, ~10 ms y 0 %.
 */
object VideoOptimizer {

    const val TARGET_MIME = "video/avc"
    const val MAX_WIDTH = 1920
    const val MAX_HEIGHT = 1080

    fun needsOptimization(info: VideoInfo): Boolean =
        info.mime != TARGET_MIME || info.width > MAX_WIDTH || info.height > MAX_HEIGHT
}
