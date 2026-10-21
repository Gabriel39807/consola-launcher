package com.consola.launcher.widgets

import com.consola.launcher.settings.PreferredPlayer

/** La identidad de un reproductor conocido: su nombre y su color, para que el widget se reconozca de un vistazo. */
data class PlayerBrand(val packageName: String, val name: String, val argb: Long, val onArgb: Long)

/** Lo justo de una sesión de medios para decidir cuál mostrar. Puro: se testea sin Android. */
data class SessionInfo(val packageName: String, val isPlaying: Boolean, val hasTitle: Boolean)

object Players {
    val SPOTIFY = PreferredPlayer.Spotify.packageName!!
    val YOUTUBE_MUSIC = PreferredPlayer.YouTubeMusic.packageName!!

    /** En este orden se sugieren si no elegiste ninguno. */
    val KNOWN = listOf(SPOTIFY, YOUTUBE_MUSIC)

    private val BRANDS = listOf(
        PlayerBrand(SPOTIFY, "Spotify", argb = 0xFF1DB954, onArgb = 0xFF000000),
        PlayerBrand(YOUTUBE_MUSIC, "YouTube Music", argb = 0xFFFF0033, onArgb = 0xFFFFFFFF),
    ).associateBy { it.packageName }

    /** null para el resto (YouTube, Netflix...): el widget usa el color de la carátula. */
    fun brandFor(packageName: String): PlayerBrand? = BRANDS[packageName]
}

object PlayerChoice {

    /**
     * Qué sesión mostrar: lo que esté SONANDO gana siempre; si nada suena, tu reproductor en pausa; si no,
     * la primera en pausa. Las sesiones sin canción (como la de un Bluetooth desconectado) no cuentan.
     */
    fun pick(sessions: List<SessionInfo>, preferredPackage: String?): Int? {
        val usable = sessions.withIndex().filter { it.value.hasTitle }
        val chosen = usable.firstOrNull { it.value.isPlaying }
            ?: usable.firstOrNull { it.value.packageName == preferredPackage }
            ?: usable.firstOrNull()
        return chosen?.index
    }

    /** Qué reproductor sugerir para abrir cuando no suena nada: el tuyo si está instalado, si no el primero que haya. */
    fun suggest(isInstalled: (String) -> Boolean, preferredPackage: String?): String? =
        preferredPackage?.takeIf(isInstalled) ?: Players.KNOWN.firstOrNull(isInstalled)
}
