package com.consola.launcher

/** Tu configuración. Editala a gusto y reinstalá. */
object LauncherConfig {

    /** Aparecen primero, en este orden. Si alguna no está instalada, se ignora. */
    val FAVORITES = listOf(
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.disney.disneyplus",
        "com.amazon.amazonvideo.livingroom",
        "com.spotify.tv.android",
    )

    /** Nunca se muestran. Para saber el paquete de una app: adb shell pm list packages */
    val HIDDEN = setOf<String>(
        // "com.google.android.videos", // app "Google TV" (tienda de pelis)
    )
}
