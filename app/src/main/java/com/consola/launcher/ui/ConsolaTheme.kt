package com.consola.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import com.consola.launcher.settings.FontChoice

@Composable
fun ConsolaTheme(font: FontChoice = FontChoice.System, content: @Composable () -> Unit) {
    // La letra elegida en Ajustes → Temas se aplica a TODA la app de una vez, desde acá.
    val typography = remember(font) { font.toFontFamily()?.let { Typography().withFamily(it) } ?: Typography() }
    MaterialTheme(colorScheme = darkColorScheme(), typography = typography) {
        // Sin una Surface de por medio, el texto saldría negro sobre fondo oscuro.
        CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
    }
}
