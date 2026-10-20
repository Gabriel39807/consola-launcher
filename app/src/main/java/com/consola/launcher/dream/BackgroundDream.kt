package com.consola.launcher.dream

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.consola.launcher.ConsolaApp
import com.consola.launcher.settings.FontChoice
import kotlinx.coroutines.flow.map
import com.consola.launcher.ui.ConsolaTheme

/**
 * El protector de pantalla: tu fondo (video o aurora) con la hora.
 *
 * Un DreamService no es una Activity: no trae el "ciclo de vida" que Compose necesita para saber cuándo
 * está visible. Se lo damos a mano. Gracias a eso, el video se libera al despertar la tele, igual que
 * en el inicio (LifecycleStartEffect en VideoBackground).
 */
class BackgroundDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Cualquier tecla del control despierta la tele: un protector no tiene botones.
        isInteractive = false
        isFullscreen = true

        val graph = (application as ConsolaApp).graph
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BackgroundDream)
            setViewTreeSavedStateRegistryOwner(this@BackgroundDream)
            setContent {
                // El protector usa la misma letra que elegiste para el Inicio.
                // remember: un Flow nuevo en cada redibujado haría que collectAsState se vuelva a suscribir sin parar.
                val fonts = remember { graph.settings.settings.map { it.font } }
                val font by fonts.collectAsState(initial = FontChoice.System)
                ConsolaTheme(font) { DreamScreen(graph) }
            }
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
