package com.consola.launcher.apps

/** Una app de TV instalada. Kotlin puro: sin Android, así se testea con JUnit plano. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val category: AppCategory = AppCategory.Other,
)
