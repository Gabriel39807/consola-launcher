package com.consola.launcher.apps

import androidx.compose.ui.graphics.ImageBitmap

/** Android TV distingue el banner (16:9, pensado para la tele) del ícono (cuadrado, heredado del celular). */
sealed interface AppArtwork {
    data class Banner(val image: ImageBitmap) : AppArtwork
    data class Icon(val image: ImageBitmap) : AppArtwork
    data object None : AppArtwork
}

/** Lo que se ve de una app: su arte y su color característico (ARGB), si se pudo sacar. */
data class AppVisuals(
    val artwork: AppArtwork,
    val accentArgb: Int?,
)

/** Una app instalada, lista para mostrar. */
data class LoadedApp(
    val app: InstalledApp,
    val visuals: AppVisuals,
)
