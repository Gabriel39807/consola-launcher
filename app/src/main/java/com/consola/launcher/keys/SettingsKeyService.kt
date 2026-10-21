package com.consola.launcher.keys

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.consola.launcher.MainActivity
import com.consola.launcher.home.Destination
import com.consola.launcher.home.SystemActions
import com.consola.launcher.quickpanel.QuickPanelOverlay

/**
 * Hace andar el botón Ajustes del control sin el inicio de Google: un toque abre el panel de ajustes rápidos
 * de Consola; mantenerlo 3 segundos abre los ajustes completos de la tele.
 *
 * Ese botón abría un panel que vive DENTRO del inicio de Google, y Android solo deja que lo reemplace una app
 * de sistema. Un servicio de accesibilidad ve las teclas ANTES que Android: atrapamos solo esa (y miramos Home
 * para cerrar el panel). No leemos la pantalla ni ninguna otra tecla. Se activa con scripts/debloat.ps1.
 */
class SettingsKeyService : AccessibilityService() {

    private val button = SettingsButton()
    private val handler = Handler(Looper.getMainLooper())
    // Los tiempos de KeyEvent están en la misma base que uptimeMillis.
    private val holdCheck = Runnable { button.onHoldCheck(SystemClock.uptimeMillis())?.let(::perform) }
    private var panel: QuickPanelOverlay? = null

    // Si la tele se apaga con el panel abierto, al prenderla no lo queremos ver colgado.
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            panel?.close(animated = false)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // La capa se arma YA, escondida: el primer toque del botón no tiene que esperar a que se cree.
        panel = QuickPanelOverlay(this).also { it.attach() }
        ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Botones que solo atendía el inicio de Google: los atiende Consola (al soltarlos, como cualquier botón).
        RemoteKeys.shortcutFor(event.keyCode)?.let { shortcut ->
            if (event.action == KeyEvent.ACTION_UP && event.repeatCount == 0) run(shortcut)
            return true
        }
        // Home con el panel abierto: lo cerramos y Home sigue su camino (te lleva al inicio igual).
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            panel?.close(animated = false)
            return false
        }
        if (!RemoteKeys.isSettingsKey(event.keyCode)) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (button.onDown(event.eventTime, event.repeatCount)) {
                handler.removeCallbacks(holdCheck)
                handler.postDelayed(holdCheck, SettingsButton.HOLD_MS)
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(holdCheck)
                button.onUp(event.eventTime)?.let(::perform)
            }
        }
        // Apretar y soltar se atrapan siempre: si dejáramos pasar alguno, Android buscaría el panel de Google.
        return true
    }

    private fun run(shortcut: RemoteShortcut) {
        when (shortcut) {
            RemoteShortcut.QuickPanel -> panel?.toggle()
            RemoteShortcut.AllApps -> {
                panel?.close(animated = false)
                startActivity(MainActivity.open(this, Destination.Apps).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            RemoteShortcut.LiveTv -> {
                panel?.close(animated = false)
                SystemActions.openLiveTv(this)
            }
        }
    }

    private fun perform(action: ButtonAction) {
        when (action) {
            ButtonAction.QuickPanel -> panel?.toggle()
            ButtonAction.FullSettings -> {
                panel?.close(animated = false)
                SystemActions.openTvSettings(this)
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(screenOff) }
        panel?.destroy()
        panel = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
