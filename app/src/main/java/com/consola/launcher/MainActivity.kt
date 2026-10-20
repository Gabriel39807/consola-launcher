package com.consola.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.consola.launcher.home.HomeScreen
import com.consola.launcher.settings.FontChoice
import com.consola.launcher.ui.ConsolaTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fonts = (application as ConsolaApp).graph.settings.settings.map { it.font }
        setContent {
            // La letra se elige en Ajustes → Temas: al cambiarla, toda la app se redibuja con la nueva.
            val font by fonts.collectAsStateWithLifecycle(initialValue = FontChoice.System)
            ConsolaTheme(font) {
                HomeScreen()
            }
        }
    }
}
