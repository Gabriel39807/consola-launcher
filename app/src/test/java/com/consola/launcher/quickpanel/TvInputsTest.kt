package com.consola.launcher.quickpanel

import org.junit.Assert.assertEquals
import org.junit.Test

class TvInputsTest {

    private fun entry(label: String, kind: InputKind) = InputEntry(id = label, label = label, kind = kind, connected = false)

    @Test
    fun `primero los HDMI en orden, despues AV y al final la antena`() {
        val raw = listOf(
            entry("TV", InputKind.Tuner),
            entry("HDMI 3", InputKind.Hdmi),
            entry("AV", InputKind.Composite),
            entry("HDMI 1", InputKind.Hdmi),
            entry("HDMI 2", InputKind.Hdmi),
        )
        assertEquals(listOf("HDMI 1", "HDMI 2", "HDMI 3", "AV", "TV"), TvInputs.ordered(raw).map { it.label })
    }

    @Test
    fun `un HDMI con nombre propio se ordena por su numero de puerto, no por el nombre`() {
        val raw = listOf(
            InputEntry(id = "b", label = "PlayStation", kind = InputKind.Hdmi, connected = true, port = 2),
            InputEntry(id = "a", label = "Deco", kind = InputKind.Hdmi, connected = false, port = 1),
        )
        assertEquals(listOf("Deco", "PlayStation"), TvInputs.ordered(raw).map { it.label })
    }
}
