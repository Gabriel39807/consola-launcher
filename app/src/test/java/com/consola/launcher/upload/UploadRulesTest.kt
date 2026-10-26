package com.consola.launcher.upload

import com.consola.launcher.widgets.Countdown
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

    @Test
    fun `un texto del celular se limpia y se corta`() {
        assertEquals("Hola mundo", UploadRules.cleanText("  Hola\n\tmundo  ", maxLength = 40))
        assertEquals("abc", UploadRules.cleanText("abcdef", maxLength = 3))
    }

    @Test
    fun `la cuenta regresiva desde el celular se valida`() {
        assertEquals(
            CountdownInput.Valid(Countdown("Vacaciones", java.time.LocalDate.of(2026, 12, 20))),
            UploadRules.countdownInput("  Vacaciones ", "2026-12-20"),
        )
        // Sin título = borrarla.
        assertEquals(CountdownInput.Clear, UploadRules.countdownInput("   ", "2026-12-20"))
        assertEquals(CountdownInput.Invalid, UploadRules.countdownInput("Vacaciones", "20/12/2026"))
        // Un título larguísimo se corta, no se rechaza.
        val long = UploadRules.countdownInput("x".repeat(80), "2026-12-20") as CountdownInput.Valid
        assertEquals(UploadRules.MAX_COUNTDOWN_LENGTH, long.countdown.title.length)
    }
}
