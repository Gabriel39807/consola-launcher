package com.consola.launcher.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.consola.launcher.R

object TileLauncher {

    fun open(context: Context, tile: HomeTile) {
        val intent = when (tile) {
            is HomeTile.App ->
                context.packageManager.getLeanbackLaunchIntentForPackage(tile.packageName)
                    ?: context.packageManager.getLaunchIntentForPackage(tile.packageName)
            HomeTile.Settings -> Intent(android.provider.Settings.ACTION_SETTINGS)
        }
        if (intent == null) {
            showError(context, tile)
            return
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            showError(context, tile)
        }
    }

    private fun showError(context: Context, tile: HomeTile) {
        Toast.makeText(context, context.getString(R.string.open_failed, tile.label), Toast.LENGTH_SHORT).show()
    }
}
