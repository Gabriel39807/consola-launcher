package com.consola.launcher.quickpanel

import androidx.annotation.StringRes
import com.consola.launcher.R

enum class QuickActionId { Inputs, Picture, Sound, Network, SleepTimer, Screensaver, Accessories, Accessibility, Personalize, AllSettings }

/**
 * Un botón del panel de ajustes rápidos. [intentAction] es la pantalla de la tele que abre;
 * null si es algo de Consola (siempre está).
 */
data class QuickAction(val id: QuickActionId, @StringRes val label: Int, val intentAction: String?)

/** Lo mismo que traía el panel de Google TV, más "Personalizar". Puro: se testea sin Android. */
object QuickActions {
    val all = listOf(
        // Lo que más se usa va primero: el foco arranca en Entradas.
        QuickAction(QuickActionId.Inputs, R.string.quick_inputs, "com.android.tv.action.VIEW_INPUTS"),
        QuickAction(QuickActionId.Picture, R.string.quick_picture, "android.settings.DISPLAY_SETTINGS"),
        QuickAction(QuickActionId.Sound, R.string.quick_sound, "android.settings.SOUND_SETTINGS"),
        QuickAction(QuickActionId.Network, R.string.quick_network, "android.settings.WIFI_SETTINGS"),
        // Las dos que siguen son de MediaTek/Google: en otras teles pueden no existir y se esconden solas.
        QuickAction(QuickActionId.SleepTimer, R.string.quick_sleep_timer, "android.settings.oem.TIMER_SETTINGS"),
        QuickAction(QuickActionId.Screensaver, R.string.quick_screensaver, "com.google.android.pano.action.SLEEP"),
        QuickAction(QuickActionId.Accessories, R.string.quick_accessories, "com.google.android.intent.action.CONNECT_INPUT"),
        QuickAction(QuickActionId.Accessibility, R.string.quick_accessibility, "android.settings.ACCESSIBILITY_SETTINGS"),
        QuickAction(QuickActionId.Personalize, R.string.quick_personalize, null),
        QuickAction(QuickActionId.AllSettings, R.string.quick_all_settings, "android.settings.SETTINGS"),
    )

    /** Los botones que de verdad abren algo en ESTA tele: un botón que no hace nada es peor que no tenerlo. */
    fun available(resolves: (String) -> Boolean): List<QuickAction> =
        all.filter { action -> action.intentAction == null || resolves(action.intentAction) }
}
