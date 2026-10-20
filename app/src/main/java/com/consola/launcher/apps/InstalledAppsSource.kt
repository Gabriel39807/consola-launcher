package com.consola.launcher.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import java.util.concurrent.ConcurrentHashMap

/**
 * Adaptador hacia el PackageManager: traduce "lo que Android tiene instalado" a nuestro modelo.
 * Solo ve las apps con LEANBACK_LAUNCHER gracias al bloque <queries> del manifest.
 */
class InstalledAppsSource(context: Context, private val covers: CoverStore) {

    private val packageManager = context.packageManager
    private val visualsCache = ConcurrentHashMap<String, AppVisuals>()

    fun installedApps(): List<InstalledApp> {
        val tvApps = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return packageManager.queryIntentActivities(tvApps, 0).map { info ->
            val appInfo = info.activityInfo.applicationInfo
            @Suppress("DEPRECATION") // FLAG_IS_GAME: los juegos viejos solo marcan esto, sin categoría.
            val declared = AppCategory.fromAndroid(appInfo.category, isGameFlag = appInfo.flags and ApplicationInfo.FLAG_IS_GAME != 0)
            InstalledApp(
                packageName = info.activityInfo.packageName,
                label = info.loadLabel(packageManager).toString(),
                category = AppCategory.forPackage(info.activityInfo.packageName, declared),
            )
        }
    }

    /** Todas las apps de TV con su arte. Es I/O: llamalo fuera del hilo principal. */
    fun loadAll(): List<LoadedApp> =
        installedApps()
            .distinctBy { it.packageName }
            .map { LoadedApp(it, visualsFor(it.packageName)) }

    fun visualsFor(packageName: String): AppVisuals {
        // Tu portada (subida desde el celular) manda sobre el banner de la app. La fecha del archivo va en la
        // clave: si la cambiás, la clave cambia y se vuelve a leer.
        val cover = covers.fileFor(packageName)
        val key = if (cover != null) "$packageName@${cover.lastModified()}" else packageName
        return visualsCache.getOrPut(key) {
            cover?.let(::loadCover) ?: loadVisuals(packageName)
        }
    }

    private fun loadCover(file: java.io.File): AppVisuals? =
        BitmapFactory.decodeFile(file.absolutePath)?.let { AppVisuals(AppArtwork.Banner(it.asImageBitmap()), accentOf(it)) }

    private fun loadVisuals(packageName: String): AppVisuals = try {
        val banner = activityBanner(packageName) ?: packageManager.getApplicationBanner(packageName)
        if (banner != null) {
            // Al tamaño REAL del banner (o más): forzarlo a otro tamaño y después estirarlo es lo que lo volvía borroso.
            val width = maxOf(banner.intrinsicWidth, BANNER_WIDTH_PX)
            val bitmap = banner.toSoftwareBitmap(width, width * BANNER_HEIGHT_PX / BANNER_WIDTH_PX)
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
