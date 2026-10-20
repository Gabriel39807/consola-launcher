package com.consola.launcher.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Typography
import com.consola.launcher.R
import com.consola.launcher.settings.FontChoice

/**
 * La familia de letras de cada opción. Montserrat, Orbitron y Playfair son fuentes "variables": un solo
 * archivo trae todos los grosores, y Compose elige el grosor pidiéndole el peso (Light, Bold...).
 * Licencias OFL en assets/licenses.
 */
fun FontChoice.toFontFamily(): FontFamily? = when (this) {
    FontChoice.System -> null
    FontChoice.Montserrat -> variable(R.font.montserrat)
    FontChoice.Orbitron -> variable(R.font.orbitron)
    FontChoice.Playfair -> variable(R.font.playfair_display)
    // Bebas Neue tiene un solo grosor: pedirle "negrita" no cambia nada, y está bien así.
    FontChoice.Bebas -> FontFamily(Font(R.font.bebas_neue))
}

private fun variable(resId: Int) = FontFamily(
    Font(resId, FontWeight.Light),
    Font(resId, FontWeight.Normal),
    Font(resId, FontWeight.Medium),
    Font(resId, FontWeight.SemiBold),
    Font(resId, FontWeight.Bold),
)

/** La misma tipografía de TV Material, pero con nuestra letra en TODOS los estilos de texto. */
fun Typography.withFamily(family: FontFamily): Typography = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)
