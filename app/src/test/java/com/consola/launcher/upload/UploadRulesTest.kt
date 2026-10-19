package com.consola.launcher.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UploadRulesTest {

    @Test
    fun `limpia espacios y simbolos del nombre`() {
        assertEquals("videoplayback-7.mp4", UploadRules.safeFileName("videoplayback (7).mp4", existing = emptySet()))
    }

    @Test
    fun `la extension se pasa a minusculas`() {
        assertEquals("itachi.mp4", UploadRules.safeFileName("Itachi.MP4", existing = emptySet()))
    }

    @Test
    fun `rechaza archivos que no son video`() {
        assertNull(UploadRules.safeFileName("virus.exe", existing = emptySet()))
        assertNull(UploadRules.safeFileName("sin-extension", existing = emptySet()))
    }

    @Test
    fun `no pisa un fondo que ya existe`() {
        assertEquals("itachi-2.mp4", UploadRules.safeFileName("itachi.mp4", existing = setOf("itachi.mp4")))
        assertEquals("itachi-3.mp4", UploadRules.safeFileName("itachi.mp4", existing = setOf("itachi.mp4", "itachi-2.mp4")))
    }

    @Test
    fun `un nombre que queda vacio pasa a llamarse fondo`() {
        assertEquals("fondo.webm", UploadRules.safeFileName("(((  ))).webm", existing = emptySet()))
    }

    @Test
    fun `no deja escapar de la carpeta con rutas`() {
        assertEquals("passwd.mp4", UploadRules.safeFileName("../../etc/passwd.mp4", existing = emptySet()))
    }

    @Test
    fun `recorta nombres larguisimos`() {
        val result = UploadRules.safeFileName("a".repeat(200) + ".mp4", existing = emptySet())!!

        assertEquals(UploadRules.MAX_BASE_NAME_LENGTH + ".mp4".length, result.length)
    }
}
