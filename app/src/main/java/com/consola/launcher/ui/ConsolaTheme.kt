package com.consola.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@Composable
fun ConsolaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        // Sin una Surface de por medio, el texto saldría negro sobre fondo oscuro.
        CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
    }
}
