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
    val layout: HomeLayout = HomeLayout.Cinema,
    val clock: ClockStyle = ClockStyle(),
    val widgets: WidgetSettings = WidgetSettings(),
    val tuning: BackgroundTuning = BackgroundTuning(),
    val font: FontChoice = FontChoice.System,
    /** Nombres propios por paquete ("Netflix" → "Pelis"). Las portadas propias viven como archivos. */
    val customNames: Map<String, String> = emptyMap(),
    /** Texto que muestra el protector de pantalla debajo de la hora. Vacío = nada. */
    val dreamMessage: String = "",
    val savedThemes: List<Theme> = emptyList(),
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

    /**
     * Guarda el orden que la persona ve en pantalla. Las favoritas que NO se ven (no instaladas)
     * conservan su lugar: si moviéramos "una posición" en la lista guardada, la app podría
     * intercambiarse con una invisible y en pantalla parecería que no se movió.
     */
    fun withVisibleOrder(visible: List<String>): LauncherSettings {
        // Solo favoritas y sin repetir: cualquier otra cosa es un orden inválido y no tocamos nada.
        if (visible.any { it !in favorites } || visible.distinct().size != visible.size) return this
        val slots = favorites.withIndex().filter { it.value in visible }.map { it.index }
        val reordered = favorites.toMutableList()
        slots.zip(visible).forEach { (slot, packageName) -> reordered[slot] = packageName }
        return copy(favorites = reordered)
    }

    companion object {
        val Default = LauncherSettings(
            favorites = LauncherConfig.FAVORITES,
            hidden = LauncherConfig.HIDDEN,
            background = null,
        )
    }
}
