package com.consola.launcher.quickpanel

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.consola.launcher.ConsolaApp
import com.consola.launcher.MainActivity
import com.consola.launcher.home.Destination
import com.consola.launcher.settings.FontChoice
import com.consola.launcher.ui.ConsolaTheme
import kotlinx.coroutines.flow.map

/**
 * El panel de ajustes rápidos como una CAPA encima de todo, dibujada por el servicio del botón Ajustes.
 *
 * Fluidez: la capa se arma UNA vez, cuando arranca el servicio, y queda escondida (invisible y sin tomar el
 * control). Abrir el panel es solo mostrarla y deslizarla: no hay que crear una pantalla ni armar la interfaz en
 * cada toque, y la app de abajo (YouTube, Netflix) sigue corriendo. Escondida no se dibuja, así que no le suma
 * trabajo a la tele mientras mirás un video.
 *
 * Como no es una Activity, el ciclo de vida que Compose necesita se lo damos a mano (igual que el protector).
 */
class QuickPanelOverlay(private val service: AccessibilityService) : LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val graph = (service.application as ConsolaApp).graph
    private var view: ComposeView? = null

    // Qué pantallas existen en esta tele no cambia entre toques: se averigua una vez.
    private val actions by lazy { QuickActions.available(::resolves) }
    private var open by mutableStateOf(false)
    // Cada apertura empieza de cero: foco en la primera entrada y la lista arriba de todo.
    private var openCount by mutableIntStateOf(0)
    private var inputs by mutableStateOf<List<InputEntry>>(emptyList())

    val isShowing: Boolean get() = open

    init {
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /** Arma la capa escondida. Lo llama el servicio al conectarse. */
    fun attach() {
        if (view != null) return
        val panel = ComposeView(service).apply {
            setViewTreeLifecycleOwner(this@QuickPanelOverlay)
            setViewTreeSavedStateRegistryOwner(this@QuickPanelOverlay)
            visibility = View.INVISIBLE
            setContent {
                val fonts = remember { graph.settings.settings.map { it.font } }
                val clocks = remember { graph.settings.settings.map { it.clock } }
                val font by fonts.collectAsState(initial = FontChoice.System)
                val clock by clocks.collectAsState(initial = null)
                ConsolaTheme(font) {
                    QuickPanel(
                        open = open,
                        openCount = openCount,
                        actions = actions,
                        inputs = inputs,
                        clock = clock,
                        nowPlaying = graph.nowPlaying,
                        onAction = ::perform,
                        onInput = ::openInput,
                        onCloseRequest = { close(animated = true) },
                        onHidden = { if (!open) hideNow() },
                    )
                }
            }
        }
        windowManager.addView(panel, layoutParams(focusable = false))
        view = panel
    }

    fun toggle() = if (open) close(animated = true) else open()

    fun open() {
        val panel = view ?: return
        if (open) return
        inputs = readInputs()
        openCount++
        open = true
        panel.visibility = View.VISIBLE
        // Recién ahora toma las flechas del control; escondida, las teclas van a la app de abajo.
        windowManager.updateViewLayout(panel, layoutParams(focusable = true))
    }

    /** [animated] = sale deslizándose (toque del botón, Back). Sin animación: Home, apagar la tele, abrir algo. */
    fun close(animated: Boolean) {
        if (!open) return
        open = false
        if (!animated) hideNow()
    }

    fun destroy() {
        view?.let(windowManager::removeView)
        view = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun hideNow() {
        val panel = view ?: return
        panel.visibility = View.INVISIBLE
        windowManager.updateViewLayout(panel, layoutParams(focusable = false))
    }

    private fun perform(action: QuickAction) {
        close(animated = false)
        start(
            when (action.id) {
                QuickActionId.Personalize -> MainActivity.open(service, Destination.Settings)
                else -> Intent(action.intentAction)
            },
        )
    }

    // Directo a la entrada, sin pasar por el menú de entradas: un clic y estás en la PlayStation.
    private fun openInput(input: InputEntry) {
        close(animated = false)
        val intent = if (input.kind == InputKind.Tuner) {
            Intent(Intent.ACTION_VIEW).setDataAndType(TvContract.buildChannelsUriForInput(input.id), TvContract.Channels.CONTENT_TYPE)
        } else {
            Intent(Intent.ACTION_VIEW).setDataAndType(
                TvContract.buildChannelUriForPassthroughInput(input.id),
                TvContract.Channels.CONTENT_ITEM_TYPE,
            )
        }
        start(intent)
    }

    private fun start(intent: Intent) {
        try {
            // Los servicios de accesibilidad pueden abrir pantallas aunque no estén a la vista.
            service.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            // No debería pasar: solo mostramos lo que resuelve.
        }
    }

    private fun readInputs(): List<InputEntry> {
        val manager = service.getSystemService(TvInputManager::class.java) ?: return emptyList()
        val entries = runCatching { manager.tvInputList }.getOrDefault(emptyList())
            .filterNot { it.isHidden(service) }
            .mapNotNull { info -> entryFor(info, manager) }
        return TvInputs.ordered(entries)
    }

    private fun entryFor(info: TvInputInfo, manager: TvInputManager): InputEntry? {
        val kind = when (info.type) {
            TvInputInfo.TYPE_HDMI -> InputKind.Hdmi
            TvInputInfo.TYPE_COMPOSITE, TvInputInfo.TYPE_COMPONENT, TvInputInfo.TYPE_SVIDEO, TvInputInfo.TYPE_SCART -> InputKind.Composite
            TvInputInfo.TYPE_TUNER -> InputKind.Tuner
            else -> return null
        }
        val defaultLabel = info.loadLabel(service).toString()
        // Si en la tele le pusiste nombre a la entrada ("PlayStation"), usamos ese.
        val label = info.loadCustomLabel(service)?.toString()?.takeIf { it.isNotBlank() } ?: defaultLabel
        return InputEntry(
            id = info.id,
            label = label,
            kind = kind,
            connected = runCatching { manager.getInputState(info.id) }.getOrNull() == TvInputManager.INPUT_STATE_CONNECTED,
            port = Regex("\\d+").find(defaultLabel)?.value?.toIntOrNull(),
        )
    }

    private fun resolves(action: String): Boolean =
        service.packageManager.resolveActivity(Intent(action), PackageManager.MATCH_DEFAULT_ONLY) != null

    private fun layoutParams(focusable: Boolean) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            // Escondida no toma ni teclas ni toques: todo sigue yendo a la app de abajo.
            (if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
        PixelFormat.TRANSLUCENT,
    )
}
