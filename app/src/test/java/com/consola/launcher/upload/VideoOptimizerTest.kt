package com.consola.launcher.upload

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoOptimizerTest {

    @Test
    fun `un 4K de YouTube se optimiza`() {
        assertTrue(VideoOptimizer.needsOptimization(VideoInfo(mime = "video/x-vnd.on2.vp9", width = 3840, height = 2160)))
    }

    @Test
    fun `un 1080p H264 ya esta listo`() {
        assertFalse(VideoOptimizer.needsOptimization(VideoInfo(mime = "video/avc", width = 1920, height = 1080)))
    }

    @Test
    fun `un 1080p en otro codec se optimiza`() {
        assertTrue(VideoOptimizer.needsOptimization(VideoInfo(mime = "video/hevc", width = 1920, height = 1080)))
    }

    @Test
    fun `un video vertical de celular mas alto que 1080 se optimiza`() {
        assertTrue(VideoOptimizer.needsOptimization(VideoInfo(mime = "video/avc", width = 1080, height = 1920)))
    }
}
