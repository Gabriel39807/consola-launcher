package com.consola.launcher.dream

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Elige qué protector de pantalla usa la tele.
 *
 * Google TV no tiene pantalla para elegir el protector (no existe android.settings.DREAM_SETTINGS),
 * pero el sistema lee la opción "screensaver_components". Escribirla requiere WRITE_SECURE_SETTINGS,
 * un permiso que no se puede pedir en pantalla: se concede UNA vez por ADB (scripts/debloat.ps1).
 */
class ScreensaverSetting(context: Context) {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val ourComponent = ComponentName(appContext, BackgroundDream::class.java)

    fun canChange(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun isActive(): Boolean =
        Settings.Secure.getString(resolver, COMPONENTS).orEmpty()
            .split(',')
            .mapNotNull { ComponentName.unflattenFromString(it.trim()) }
            .contains(ourComponent)

    /** Activa el nuestro o vuelve al que trae la tele de fábrica. */
    fun setActive(active: Boolean) {
        if (!canChange()) return
        val value = if (active) ourComponent.flattenToString() else Settings.Secure.getString(resolver, DEFAULT_COMPONENT)
        Settings.Secure.putString(resolver, COMPONENTS, value)
        if (active) Settings.Secure.putInt(resolver, ENABLED, 1)
    }

    private companion object {
        // Claves internas de Android (Settings.Secure las tiene ocultas en el SDK público).
        const val COMPONENTS = "screensaver_components"
        const val ENABLED = "screensaver_enabled"
        const val DEFAULT_COMPONENT = "screensaver_default_component"
    }
}
