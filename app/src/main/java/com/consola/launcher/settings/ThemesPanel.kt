package com.consola.launcher.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.ui.toFontFamily
import com.consola.launcher.ui.tvClickable

private val CardShape = RoundedCornerShape(14.dp)

/** Un color por tema de fábrica, para que cada tarjeta se reconozca de un vistazo. */
private val ThemeColors = mapOf(
    "cine" to Color(0xFFB71C1C),
    "gamer" to Color(0xFF00C853),
    "zen" to Color(0xFF00897B),
    "arcade" to Color(0xFFAA00FF),
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ThemesPanel(
    settings: LauncherSettings,
    onApply: (Theme) -> Unit,
    onSaveCurrent: () -> Unit,
    onDelete: (Theme) -> Unit,
    onFont: (FontChoice) -> Unit,
) {
    // Para marcar "En uso": un tema está puesto si su look coincide con el actual (sin importar id ni nombre).
    val currentLook = Theme.from(settings, id = "", name = "")
    val isCurrent: (Theme) -> Boolean = { it.copy(id = "", name = "") == currentLook }

    LazyColumn(
        modifier = Modifier.fillMaxSize().focusRestorer(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item { PanelTitle(stringResource(R.string.settings_themes)) }
        item {
            Text(stringResource(R.string.themes_builtin), style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.7f))
            ThemeRow(Themes.builtIn, isCurrent, onApply, onLongPress = null)
        }
        item {
            SettingsRow(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.theme_font),
                value = stringResource(settings.font.label),
                onClick = { onFont(settings.font.next()) },
            )
        }
        item {
            SettingsRow(
                icon = Icons.Default.Add,
                title = stringResource(R.string.theme_save),
                description = stringResource(R.string.theme_save_desc),
                onClick = onSaveCurrent,
            )
        }
        if (settings.savedThemes.isNotEmpty()) {
            item {
                PanelTitle(stringResource(R.string.themes_mine), Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.theme_delete_hint), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                ThemeRow(settings.savedThemes, isCurrent, onApply, onLongPress = onDelete)
            }
        }
    }
}

@Composable
private fun ThemeRow(themes: List<Theme>, isCurrent: (Theme) -> Boolean, onApply: (Theme) -> Unit, onLongPress: ((Theme) -> Unit)?) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(themes, key = { it.id }) { theme ->
            ThemeCard(
                theme = theme,
                isCurrent = isCurrent(theme),
                onClick = { onApply(theme) },
                onLongClick = onLongPress?.let { { it(theme) } },
            )
        }
    }
}

/** La tarjeta muestra el nombre del tema escrito CON SU letra: la mejor vista previa es la de verdad. */
@Composable
private fun ThemeCard(theme: Theme, isCurrent: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, label = "themeScale")
    val color = ThemeColors[theme.id] ?: SettingsAccent
    val family = remember(theme.font) { theme.font.toFontFamily() }

    Box(
        Modifier
            .size(190.dp, 106.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CardShape)
            .background(Brush.linearGradient(listOf(color, Color(0xFF0B0F1A))))
            .then(if (isFocused) Modifier.border(3.dp, Color.White, CardShape) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .tvClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
    ) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(
                text = theme.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = family ?: MaterialTheme.typography.headlineSmall.fontFamily),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(theme.layout.layoutName), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
        }
        if (isCurrent) {
            Text(
                text = stringResource(R.string.bg_active),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

private val HomeLayout.layoutName: Int
    get() = when (this) {
        HomeLayout.Cinema -> R.string.layout_cinema
        HomeLayout.Ps5 -> R.string.layout_ps5
        HomeLayout.Xbox -> R.string.layout_xbox
        HomeLayout.Minimal -> R.string.layout_minimal
        HomeLayout.Rows -> R.string.layout_rows
    }

private val FontChoice.label: Int
    get() = when (this) {
        FontChoice.System -> R.string.font_system
        FontChoice.Montserrat -> R.string.font_montserrat
        FontChoice.Orbitron -> R.string.font_orbitron
        FontChoice.Playfair -> R.string.font_playfair
        FontChoice.Bebas -> R.string.font_bebas
    }
