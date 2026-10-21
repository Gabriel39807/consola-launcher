package com.consola.launcher.keys

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKeysTest {

    @Test
    fun `el boton Ajustes se reconoce como SETTINGS y como FOCUS de las teles KTC`() {
        assertTrue(RemoteKeys.isSettingsKey(KeyEvent.KEYCODE_SETTINGS))
        assertTrue(RemoteKeys.isSettingsKey(KeyEvent.KEYCODE_FOCUS))
    }

    @Test
    fun `las demas teclas siguen su camino`() {
        for (key in listOf(KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_TV_INPUT)) {
            assertFalse(RemoteKeys.isSettingsKey(key))
            assertNull(RemoteKeys.shortcutFor(key))
        }
    }

    @Test
    fun `los botones que solo atendia el inicio de Google ahora los atiende Consola`() {
        assertEquals(RemoteShortcut.AllApps, RemoteKeys.shortcutFor(KeyEvent.KEYCODE_ALL_APPS))
        assertEquals(RemoteShortcut.LiveTv, RemoteKeys.shortcutFor(KeyEvent.KEYCODE_GUIDE))
        assertEquals(RemoteShortcut.QuickPanel, RemoteKeys.shortcutFor(KeyEvent.KEYCODE_NOTIFICATION))
    }

    @Test
    fun `el boton Ajustes no es un atajo simple porque distingue toque de mantener`() {
        assertNull(RemoteKeys.shortcutFor(KeyEvent.KEYCODE_SETTINGS))
        assertNull(RemoteKeys.shortcutFor(KeyEvent.KEYCODE_FOCUS))
    }
}

class SettingsButtonTest {

    private val button = SettingsButton(holdMs = 3_000)

    @Test
    fun `un toque corto abre el panel rapido al soltar`() {
        assertTrue(button.onDown(now = 1_000, repeatCount = 0))
        assertEquals(ButtonAction.QuickPanel, button.onUp(now = 1_200))
    }

    @Test
    fun `mantenerlo 3 segundos abre los ajustes completos sin esperar a soltarlo`() {
        button.onDown(now = 1_000, repeatCount = 0)
        assertNull("antes de los 3 s no pasa nada", button.onHoldCheck(now = 3_500))
        assertEquals(ButtonAction.FullSettings, button.onHoldCheck(now = 4_000))
        assertNull("al soltar ya no abre el panel", button.onUp(now = 4_800))
    }

    @Test
    fun `las repeticiones del control mientras se mantiene no cuentan como toques nuevos`() {
        button.onDown(now = 1_000, repeatCount = 0)
        assertFalse(button.onDown(now = 1_500, repeatCount = 1))
        assertFalse(button.onDown(now = 2_000, repeatCount = 2))
        assertEquals(ButtonAction.FullSettings, button.onHoldCheck(now = 4_000))
    }

    @Test
    fun `si el chequeo de los 3 s se atrasa, soltar tarde igual abre los ajustes completos`() {
        button.onDown(now = 1_000, repeatCount = 0)
        assertEquals(ButtonAction.FullSettings, button.onUp(now = 4_200))
    }

    @Test
    fun `un soltar sin apretar no hace nada`() {
        assertNull(button.onUp(now = 500))
    }

    @Test
    fun `si se perdio un soltar, el siguiente toque empieza de cero`() {
        button.onDown(now = 1_000, repeatCount = 0)
        assertTrue(button.onDown(now = 9_000, repeatCount = 0))
        assertEquals(ButtonAction.QuickPanel, button.onUp(now = 9_100))
    }
}
