package com.consola.launcher.apps

/** La fila donde va una app en el estilo "Filas". El orden del enum es el orden de las filas. */
enum class AppCategory {
    Video, Music, Games, Other;

    companion object {
        // Los valores de ApplicationInfo.CATEGORY_*: copiados acá para que esto sea Kotlin puro y testeable.
        const val ANDROID_UNDEFINED = -1
        const val ANDROID_GAME = 0
        const val ANDROID_AUDIO = 1
        const val ANDROID_VIDEO = 2

        fun fromAndroid(category: Int, isGameFlag: Boolean): AppCategory = when {
            category == ANDROID_GAME || isGameFlag -> Games
            category == ANDROID_AUDIO -> Music
            category == ANDROID_VIDEO -> Video
            else -> Other
        }

        // Muchas apps de TV no declaran categoría. Para las más comunes, la sabemos nosotros.
        private val KNOWN = mapOf(
            "com.netflix.ninja" to Video,
            "com.google.android.youtube.tv" to Video,
            "com.disney.disneyplus" to Video,
            "com.amazon.amazonvideo.livingroom" to Video,
            "com.spotify.tv.android" to Music,
            "com.google.android.youtube.tvmusic" to Music,
        )

        /** Lo que declara la app manda; si no declara nada ([declared] = Other), usamos lo conocido. */
        fun forPackage(packageName: String, declared: AppCategory): AppCategory =
            if (declared != Other) declared else KNOWN[packageName] ?: Other
    }
}
