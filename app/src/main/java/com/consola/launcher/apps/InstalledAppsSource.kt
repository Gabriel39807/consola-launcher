package com.consola.launcher.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import java.util.concurrent.ConcurrentHashMap

/**
 * Adaptador hacia el PackageManager: traduce "lo que Android tiene instalado" a nuestro modelo.
 * Solo ve las apps con LEANBACK_LAUNCHER gracias al bloque <queries> del manifest.
 */
class InstalledAppsSource(context: Context) {

    private val packageManager = context.packageManager
    private val visualsCache = ConcurrentHashMap<String, AppVisuals>()

    fun installedApps(): List<InstalledApp> {
        val tvApps = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return packageManager.queryIntentActivities(tvApps, 0).map { info ->
            InstalledApp(
                packageName = info.activityInfo.packageName,
                label = info.loadLabel(packageManager).toString(),
            )
        }
    }

    fun visualsFor(packageName: String): AppVisuals =
        visualsCache.getOrPut(packageName) { loadVisuals(packageName) }

    private fun loadVisuals(packageName: String): AppVisuals = try {
        val banner = activityBanner(packageName) ?: packageManager.getApplicationBanner(packageName)
        if (banner != null) {
            val bitmap = banner.toSoftwareBitmap(BANNER_WIDTH_PX, BANNER_HEIGHT_PX)
            AppVisuals(AppArtwork.Banner(bitmap.asImageBitmap()), accentOf(bitmap))
        } else {
            val bitmap = packageManager.getApplicationIcon(packageName).toSoftwareBitmap(ICON_SIZE_PX, ICON_SIZE_PX)
            AppVisuals(AppArtwork.Icon(bitmap.asImageBitmap()), accentOf(bitmap))
        }
    } catch (e: PackageManager.NameNotFoundException) {
        // Se desinstaló entre que la listamos y que fuimos a buscar su arte.
        AppVisuals(AppArtwork.None, accentArgb = null)
    }

    // Muchas apps declaran el banner en la actividad y no en la aplicación: por eso probamos primero acá.
    private fun activityBanner(packageName: String): Drawable? =
        packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?.let { packageManager.getActivityBanner(it) }

    // Palette necesita leer píxeles: con ARGB_8888 explícito nunca recibe un bitmap de hardware (que no se puede leer).
    private fun Drawable.toSoftwareBitmap(width: Int, height: Int): Bitmap =
        toBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Vibrante primero: en el banner de Netflix el dominante es el negro del fondo; el vibrante, el rojo del logo.
    private fun accentOf(bitmap: Bitmap): Int? {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        return (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb
    }

    private companion object {
        const val BANNER_WIDTH_PX = 320
        const val BANNER_HEIGHT_PX = 180
        const val ICON_SIZE_PX = 192
    }
}
