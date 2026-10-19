package com.consola.launcher.apps

/**
 * Decide QUÉ apps se muestran y en QUÉ orden.
 * No sabe nada de PackageManager ni de Compose: es la regla de negocio del launcher, aislada.
 */
object AppCatalog {

    fun arrange(
        apps: List<InstalledApp>,
        selfPackage: String,
        favorites: List<String>,
        hidden: Set<String>,
    ): List<InstalledApp> {
        val favoriteRank = favorites.withIndex().associate { (index, packageName) -> packageName to index }

        return apps
            .asSequence()
            .filter { it.packageName != selfPackage && it.packageName !in hidden }
            .distinctBy { it.packageName }
            .sortedWith(
                compareBy<InstalledApp>(
                    { favoriteRank[it.packageName] ?: Int.MAX_VALUE },
                    { it.label.lowercase() },
                ),
            )
            .toList()
    }
}
