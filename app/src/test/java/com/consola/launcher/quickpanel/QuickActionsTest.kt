package com.consola.launcher.quickpanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickActionsTest {

    @Test
    fun `entradas, imagen y sonido van primero porque son lo que mas se usa`() {
        val ids = QuickActions.all.map { it.id }
        assertEquals(listOf(QuickActionId.Inputs, QuickActionId.Picture, QuickActionId.Sound), ids.take(3))
    }

    @Test
    fun `se esconden los botones cuya pantalla no existe en esta tele`() {
        val missing = setOf(QuickActions.all.first { it.id == QuickActionId.SleepTimer }.intentAction)
        val shown = QuickActions.available { action -> action !in missing }
        assertTrue(shown.none { it.id == QuickActionId.SleepTimer })
        assertEquals(QuickActions.all.size - 1, shown.size)
    }

    @Test
    fun `personalizar es de Consola y se muestra siempre`() {
        val shown = QuickActions.available { false }
        assertEquals(listOf(QuickActionId.Personalize), shown.map { it.id })
    }
}
