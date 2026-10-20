package com.consola.launcher.home

import android.net.Uri
import com.consola.launcher.apps.AppArtwork
import com.consola.launcher.apps.AppCategory
import com.consola.launcher.settings.LauncherSettings

/** Una app tal como se dibuja en el inicio o en la grilla de Apps. */
data class HomeTile(
    val packageName: String,
    val label: String,
    val artwork: AppArtwork,
    /** Color característico (ARGB) que tiñe el fondo cuando el tile tiene el foco. null = color por defecto. */
    val accentArgb: Int?,
    val isFavorite: Boolean,
    val category: AppCategory = AppCategory.Other,
) {
    val key: String get() = packageName
}

data class HomeUiState(
    /** Inicio: las favoritas, en el orden elegido. */
    val homeTiles: List<HomeTile> = emptyList(),
    /** Apps: todas menos las ocultas, alfabéticas. */
    val allApps: List<HomeTile> = emptyList(),
    val backgroundVideo: Uri? = null,
    /** Estilo, reloj y widgets: todo lo que se elige en Ajustes. */
    val settings: LauncherSettings = LauncherSettings.Default,
)
