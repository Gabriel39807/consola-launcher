package com.consola.launcher.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanAddressTest {

    @Test
    fun `prefiere la red de casa tipica`() {
        assertEquals("192.168.20.72", LanAddress.pick(listOf("10.0.0.5", "192.168.20.72")))
    }

    @Test
    fun `si no hay 192_168 acepta otras redes privadas`() {
        assertEquals("10.0.0.5", LanAddress.pick(listOf("10.0.0.5")))
        assertEquals("172.20.1.4", LanAddress.pick(listOf("172.20.1.4")))
    }

    @Test
    fun `ignora direcciones que el celular no puede alcanzar`() {
        // 169.254 = sin DHCP, 127 = la propia tele, 172.32 = fuera del rango privado.
        assertNull(LanAddress.pick(listOf("169.254.3.1", "127.0.0.1", "172.32.0.1")))
    }
}
