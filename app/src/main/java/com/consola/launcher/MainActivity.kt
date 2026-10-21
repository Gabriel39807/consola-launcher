package com.consola.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.consola.launcher.home.Destination
import com.consola.launcher.home.HomeScreen
import com.consola.launcher.settings.FontChoice
import com.consola.launcher.ui.ConsolaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    // "Personalizar" del panel rápido pide abrir directo en Ajustes; el botón "Todas las apps", en Apps. Como es
    // singleTask, puede llegar con la app ya abierta (onNewIntent): es un pedido que la pantalla atiende y después baja.
    private val openRequest = MutableStateFlow<Destination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openRequest.value = intent.destination()
        val fonts = (application as ConsolaApp).graph.settings.settings.map { it.font }
        setContent {
            // La letra se elige en Ajustes → Temas: al cambiarla, toda la app se redibuja con la nueva.
            val font by fonts.collectAsStateWithLifecycle(initialValue = FontChoice.System)
            val open by openRequest.collectAsStateWithLifecycle()
            ConsolaTheme(font) {
                HomeScreen(openRequest = open, onOpenHandled = { openRequest.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home con Consola ya abierta (en Ajustes, en Apps...): volvés al Inicio, como en cualquier tele.
        val requested = intent.destination() ?: Destination.Home.takeIf { intent.hasCategory(Intent.CATEGORY_HOME) }
        requested?.let { openRequest.value = it }
    }

    private fun Intent.destination(): Destination? =
        getStringExtra(EXTRA_DESTINATION)?.let { name -> Destination.entries.firstOrNull { it.name == name } }

    companion object {
        private const val EXTRA_DESTINATION = "destination"

        fun open(context: Context, destination: Destination): Intent =
            Intent(context, MainActivity::class.java).putExtra(EXTRA_DESTINATION, destination.name)
    }
}
