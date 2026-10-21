package com.consola.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Pausa y saltear no vienen en material-icons-core, y el paquete "extended" pesa varios MB por
 * tres íconos. Los dibujamos con los mismos trazos de Material (viewport de 24×24).
 */
object MediaIcons {
    val Pause: ImageVector = materialIcon("Pause", "M6,19h4V5H6v14zM14,5v14h4V5h-4z")
    val SkipNext: ImageVector = materialIcon("SkipNext", "M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z")
    val SkipPrevious: ImageVector = materialIcon("SkipPrevious", "M6,6h2v12H6zM9.5,12l8.5,6V6z")
}

/** Un ícono de Material a partir de su trazo (viewport de 24×24). Lo usan también los del panel rápido. */
internal fun materialIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(pathData = PathParser().parsePathString(pathData).toNodes(), fill = SolidColor(Color.Black))
        .build()
