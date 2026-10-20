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

    /** Los estilos se guardan por nombre. Uno desconocido (de una versión vieja o futura) vuelve al de fábrica. */
    inline fun <reified T : Enum<T>> decodeEnum(raw: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == raw } ?: default

    fun decodeBackground(raw: String?): BackgroundChoice? = when {
        raw == AURORA -> BackgroundChoice.Aurora
        raw != null && raw.startsWith(VIDEO_PREFIX) -> BackgroundChoice.Video(raw.removePrefix(VIDEO_PREFIX))
        else -> null
    }

    // ---------- Nombres propios: una app por línea, "paquete<TAB>nombre" ----------

    fun encodeNames(names: Map<String, String>): String =
        names.entries.joinToString(LINE) { (packageName, label) -> packageName + TAB + clean(label) }

    fun decodeNames(raw: String): Map<String, String> =
        raw.lineSequence()
            .mapNotNull { line ->
                val tab = line.indexOf(TAB)
                if (tab <= 0) null else line.substring(0, tab) to line.substring(tab + 1)
            }
            .filter { it.second.isNotBlank() }
            .toMap()

    // ---------- Temas: un tema por línea, campos separados por "|" ----------

    private const val THEME_FIELDS = 13

    fun encodeTheme(theme: Theme): String = listOf(
        theme.id,
        theme.name.replace("|", " "),
        theme.layout.name,
        theme.clock.use24h,
        theme.clock.showSeconds,
        theme.clock.date.name,
        theme.clock.weight.name,
        theme.clock.size.name,
        theme.widgets.nowPlaying,
        theme.widgets.bigClock,
        theme.dim.name,
        theme.musicReactive,
        theme.font.name,
    ).joinToString("|") { clean(it.toString()) }

    /** null si la línea no es un tema válido: un dato roto no puede romper la app. */
    fun decodeTheme(raw: String): Theme? {
        val f = raw.split('|')
        if (f.size != THEME_FIELDS) return null
        return Theme(
            id = f[0],
            name = f[1],
            layout = decodeEnum(f[2], HomeLayout.Cinema),
            clock = ClockStyle(
                use24h = f[3].toBooleanStrictOrNull() ?: return null,
                showSeconds = f[4].toBooleanStrictOrNull() ?: return null,
                date = decodeEnum(f[5], DateStyle.Full),
                weight = decodeEnum(f[6], ClockWeight.Regular),
                size = decodeEnum(f[7], ClockSize.Medium),
            ),
            widgets = WidgetSettings(
                nowPlaying = f[8].toBooleanStrictOrNull() ?: return null,
                bigClock = f[9].toBooleanStrictOrNull() ?: return null,
            ),
            dim = decodeEnum(f[10], DimLevel.None),
            musicReactive = f[11].toBooleanStrictOrNull() ?: return null,
            font = decodeEnum(f[12], FontChoice.System),
        )
    }

    fun encodeThemes(themes: List<Theme>): String = themes.joinToString(LINE, transform = ::encodeTheme)

    fun decodeThemes(raw: String): List<Theme> = raw.lineSequence().mapNotNull(::decodeTheme).toList()

    private const val LINE = "\n"
    private const val TAB = "\t"
    private val CONTROL_CHARS = Regex("[\\t\\n\\r]+")

    /** Un nombre o tema no puede traer tabuladores ni saltos de línea: son nuestros separadores. */
    private fun clean(text: String): String = text.replace(CONTROL_CHARS, " ").trim()
}
