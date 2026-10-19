package com.consola.launcher.home

import android.net.Uri
import com.consola.launcher.apps.AppArtwork

sealed interface HomeTile {
    val key: String
    val label: String

    /** Color característico (ARGB) que tiñe el fondo cuando el tile tiene el foco. null = color por defecto. */
    val accentArgb: Int?

    data class App(
        val packageName: String,
        override val label: String,
        val artwork: AppArtwork,
        override val accentArgb: Int?,
        val isFavorite: Boolean,
    ) : HomeTile {
        override val key: String get() = packageName
    }

    /** Sin el home de Google, Ajustes no tiene otra puerta de entrada: por eso es un tile fijo. */
    data object Settings : HomeTile {
        override val key = "system:settings"
        override val label = "Ajustes"
        override val accentArgb = 0xFF607D8B.toInt()
    }
}

data class HomeUiState(
    val tiles: List<HomeTile> = emptyList(),
    val backgroundVideo: Uri? = null,
)
