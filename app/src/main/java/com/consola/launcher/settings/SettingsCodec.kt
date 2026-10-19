package com.consola.launcher.settings

/** Traduce los ajustes a texto para DataStore y de vuelta. Puro: se testea sin Android. */
object SettingsCodec {

    // Los nombres de paquete de Android nunca llevan comas.
    private const val SEPARATOR = ","
    private const val AURORA = "aurora"
    private const val VIDEO_PREFIX = "video:"

    // Una lista y no un Set: el orden de los favoritos es el orden en que aparecen en el inicio.
    fun encodeFavorites(favorites: List<String>): String = favorites.joinToString(SEPARATOR)

    fun decodeFavorites(raw: String): List<String> = raw.split(SEPARATOR).filter { it.isNotBlank() }

    fun encodeBackground(choice: BackgroundChoice): String = when (choice) {
        BackgroundChoice.Aurora -> AURORA
        is BackgroundChoice.Video -> VIDEO_PREFIX + choice.fileName
    }

    fun decodeBackground(raw: String?): BackgroundChoice? = when {
        raw == AURORA -> BackgroundChoice.Aurora
        raw != null && raw.startsWith(VIDEO_PREFIX) -> BackgroundChoice.Video(raw.removePrefix(VIDEO_PREFIX))
        else -> null
    }
}
