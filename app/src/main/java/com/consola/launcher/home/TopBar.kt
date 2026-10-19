package com.consola.launcher.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class Destination(@StringRes val label: Int, val icon: ImageVector?) {
    Search(R.string.tab_search, Icons.Default.Search),
    Home(R.string.tab_home, null),
    Apps(R.string.tab_apps, null),
    Settings(R.string.tab_settings, Icons.Default.Settings),
}

private val PillShape = RoundedCornerShape(50)

/**
 * Decide si las pestañas pueden recibir foco. NO es estado de Compose a propósito: Compose consulta
 * focusProperties en el momento de mover el foco, así que un cambio acá vale al instante, sin recomponer.
 *
 * Por qué existe: al subir desde una app, la búsqueda de foco 2D elegía la pestaña más cercana en línea
 * recta (desde Spotify, Ajustes) y te cambiaba de pestaña sin querer. focusRestorer() no lo evitó: medido en la tele.
 */
class TabFocusGate {
    var tabsFocusable: Boolean = true
}

@Composable
fun TopBar(
    selected: Destination,
    onSelect: (Destination) -> Unit,
    focusRequesters: Map<Destination, FocusRequester>,
    gate: TabFocusGate,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(Destination.Search, Destination.Home, Destination.Apps, Destination.Settings).forEach { destination ->
                NavTab(
                    destination = destination,
                    isSelected = destination == selected,
                    onSelect = { onSelect(destination) },
                    focusRequester = focusRequesters.getValue(destination),
                    gate = gate,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Clock()
    }
}

/** Como en Google TV: la pestaña cambia apenas le llega el foco, sin apretar OK. */
@Composable
private fun NavTab(
    destination: Destination,
    isSelected: Boolean,
    onSelect: () -> Unit,
    focusRequester: FocusRequester,
    gate: TabFocusGate,
) {
    var isFocused by remember { mutableStateOf(false) }
    val content = when {
        isFocused -> Color.Black
        isSelected -> Color.White
        else -> Color.White.copy(alpha = 0.6f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .focusProperties { canFocus = gate.tabsFocusable }
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                    if (state.isFocused) onSelect()
                }
                .clip(PillShape)
                .background(if (isFocused) Color.White else Color.Transparent)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSelect)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destination.icon?.let { icon ->
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = stringResource(destination.label),
                color = content,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
            )
        }
        // La rayita marca dónde estás aunque el foco esté en otro lado (en una app, por ejemplo).
        Box(
            Modifier
                .padding(top = 4.dp)
                .size(width = 16.dp, height = 3.dp)
                .clip(PillShape)
                .background(if (isSelected && !isFocused) Color.White else Color.Transparent),
        )
    }
}

@Composable
private fun Clock() {
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEEE d MMMM") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    Column(horizontalAlignment = Alignment.End) {
        Text(now.format(timeFormat), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            text = now.format(dayFormat).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}
