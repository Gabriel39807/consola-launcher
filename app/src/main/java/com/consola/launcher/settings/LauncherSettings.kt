package com.consola.launcher.settings

import com.consola.launcher.LauncherConfig

/** Qué fondo eligió la persona. null en [LauncherSettings.background] = automático (el video más nuevo). */
sealed interface BackgroundChoice {
    data object Aurora : BackgroundChoice
    data class Video(val fileName: String) : BackgroundChoice
}

/**
 * Todo lo que se puede configurar desde Ajustes. Inmutable: cada cambio devuelve una copia nueva,
 * así el que lo guarda (DataStore) y el que lo muestra (Compose) nunca ven un estado a medias.
 */
data class LauncherSettings(
    val favorites: List<String>,
    val hidden: Set<String>,
    val background: BackgroundChoice?,
) {
    // Favorita y oculta a la vez no tiene sentido: marcar una cosa desmarca la otra.
    fun toggleFavorite(packageName: String): LauncherSettings =
        if (packageName in favorites) {
            copy(favorites = favorites - packageName)
        } else {
            copy(favorites = favorites + packageName, hidden = hidden - packageName)
        }

    fun toggleHidden(packageName: String): LauncherSettings =
        if (packageName in hidden) {
            copy(hidden = hidden - packageName)
        } else {
            copy(hidden = hidden + packageName, favorites = favorites - packageName)
        }

    companion object {
        val Default = LauncherSettings(
            favorites = LauncherConfig.FAVORITES,
            hidden = LauncherConfig.HIDDEN,
            background = null,
        )
    }
}
