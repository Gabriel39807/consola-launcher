package com.consola.launcher.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import com.consola.launcher.R

/** Todo lo que el launcher abre FUERA de sí mismo: apps, la búsqueda de Google y los ajustes de la tele. */
object SystemActions {

    fun openApp(context: Context, tile: HomeTile) {
        val intent = context.packageManager.getLeanbackLaunchIntentForPackage(tile.packageName)
            ?: context.packageManager.getLaunchIntentForPackage(tile.packageName)
        launch(context, intent, context.getString(R.string.open_failed, tile.label))
    }

    /**
     * El Asistente de Google TV (com.google.android.katniss) atiende ASSIST: abre la búsqueda por voz
     * ENCIMA del launcher, con micrófono, teclado y sugerencias. No depende del inicio de Google.
     */
    fun openGoogleSearch(context: Context) =
        launch(context, Intent(Intent.ACTION_ASSIST), context.getString(R.string.search_unavailable))

    fun openTvSettings(context: Context) =
        launch(context, Intent(Settings.ACTION_SETTINGS), context.getString(R.string.open_failed, context.getString(R.string.tv_settings)))

    private fun launch(context: Context, intent: Intent?, errorMessage: String) {
        if (intent == null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
