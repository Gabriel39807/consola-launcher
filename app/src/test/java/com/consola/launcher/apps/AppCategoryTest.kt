package com.consola.launcher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryTest {

    @Test
    fun `las categorias de Android se traducen a filas`() {
        assertEquals(AppCategory.Games, AppCategory.fromAndroid(AppCategory.ANDROID_GAME, isGameFlag = false))
        assertEquals(AppCategory.Music, AppCategory.fromAndroid(AppCategory.ANDROID_AUDIO, isGameFlag = false))
        assertEquals(AppCategory.Video, AppCategory.fromAndroid(AppCategory.ANDROID_VIDEO, isGameFlag = false))
    }

    @Test
    fun `un juego viejo que solo marca isGame igual va a Juegos`() {
        assertEquals(AppCategory.Games, AppCategory.fromAndroid(AppCategory.ANDROID_UNDEFINED, isGameFlag = true))
    }

    @Test
    fun `lo que no declara categoria va a Mas apps`() {
        assertEquals(AppCategory.Other, AppCategory.fromAndroid(AppCategory.ANDROID_UNDEFINED, isGameFlag = false))
        assertEquals(AppCategory.Other, AppCategory.fromAndroid(7, isGameFlag = false))
    }

    @Test
    fun `las apps conocidas que no declaran categoria se ubican igual`() {
        assertEquals(AppCategory.Video, AppCategory.forPackage("com.netflix.ninja", AppCategory.Other))
        assertEquals(AppCategory.Music, AppCategory.forPackage("com.spotify.tv.android", AppCategory.Other))
        // Si la app SÍ declara algo, eso manda.
        assertEquals(AppCategory.Games, AppCategory.forPackage("com.netflix.ninja", AppCategory.Games))
    }
}
