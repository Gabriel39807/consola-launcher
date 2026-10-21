package com.consola.launcher.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.ui.TvSafeArea

enum class SettingsSection(@StringRes val label: Int) {
    Style(R.string.settings_style),
    Themes(R.string.settings_themes),
    Backgrounds(R.string.settings_backgrounds),
    Clock(R.string.settings_clock),
    Widgets(R.string.settings_widgets),
    Apps(R.string.settings_apps),
    System(R.string.settings_system),
}

/** Dos paneles, como los ajustes de cualquier tele: menú a la izquierda, contenido a la derecha. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audioStatus by viewModel.audioStatus.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(SettingsSection.Style) }
    val menuFocus = remember { SettingsSection.entries.associateWith { FocusRequester() } }

    // El servidor para el celular vive mientras estés en Ajustes (en cualquier sección) y se cierra al salir:
    // nada de puertos abiertos de más en tu red, pero tampoco tenés que ir a Fondos para cambiar el nombre de una app.
    DisposableEffect(viewModel) {
        viewModel.startUploads()
        onDispose { viewModel.stopUploads() }
    }

    Row(
        Modifier
            .fillMaxSize()
            // Bajando desde las pestañas se entra al MENÚ, en la sección donde estás, no al panel de la derecha:
            // primero elegís qué ajustar y recién después entrás. La búsqueda de foco normal elegía lo más cercano.
            .focusProperties {
                enter = { direction -> if (direction == FocusDirection.Down) menuFocus.getValue(section) else FocusRequester.Default }
            }
            .focusGroup()
            .padding(start = TvSafeArea.Horizontal, end = TvSafeArea.Horizontal, bottom = TvSafeArea.Vertical),
    ) {
        SectionMenu(selected = section, onSelect = { section = it }, focusRequesters = menuFocus, modifier = Modifier.width(210.dp))
        Spacer(Modifier.width(40.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                // IZQUIERDA desde el panel vuelve a la sección donde estabas, no a la que quede a la misma altura:
                // si bajaste por los estilos, la más cercana sería "Widgets" y te cambiaría de sección sin querer.
                .focusProperties {
                    exit = { direction -> if (direction == FocusDirection.Left) menuFocus.getValue(section) else FocusRequester.Default }
                }
                .focusGroup(),
        ) {
            when (section) {
                SettingsSection.Style -> StylePanel(current = state.settings.layout, onSelect = viewModel::setLayout)
                SettingsSection.Themes -> {
                    val prefix = stringResource(R.string.theme_my_prefix)
                    ThemesPanel(
                        settings = state.settings,
                        onApply = viewModel::applyTheme,
                        onSaveCurrent = { viewModel.saveCurrentAsTheme(prefix) },
                        onDelete = viewModel::deleteTheme,
                        onFont = viewModel::setFont,
                    )
                }
                SettingsSection.Clock -> ClockPanel(style = state.settings.clock, onChange = viewModel::updateClock)
                SettingsSection.Widgets -> WidgetsPanel(widgets = state.settings.widgets, onChange = viewModel::updateWidgets)
                SettingsSection.Backgrounds -> BackgroundsPanel(
                    backgrounds = state.backgrounds,
                    upload = state.upload,
                    tuning = state.settings.tuning,
                    audioStatus = audioStatus,
                    hasAudioPermission = viewModel::hasAudioPermission,
                    onSelect = viewModel::selectBackground,
                    onTuningChange = viewModel::updateTuning,
                    onCycleDayNight = viewModel::cycleDayNight,
                )
                SettingsSection.Apps -> AppsPanel(
                    rows = state.apps,
                    upload = state.upload,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onToggleHidden = viewModel::toggleHidden,
                )
                SettingsSection.System -> SystemPanel()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SectionMenu(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    focusRequesters: Map<SettingsSection, FocusRequester>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.focusRestorer(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SettingsSection.entries.forEach { section ->
            MenuItem(
                label = section.label,
                isSelected = section == selected,
                // Igual que las pestañas: la sección cambia apenas le llega el foco.
                onFocused = { onSelect(section) },
                modifier = Modifier.focusRequester(focusRequesters.getValue(section)),
            )
        }
    }
}

@Composable
private fun MenuItem(@StringRes label: Int, isSelected: Boolean, onFocused: () -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = when {
            isFocused -> Color.Black
            isSelected -> Color.White
            else -> Color.White.copy(alpha = 0.6f)
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) onFocused()
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isFocused -> Color.White
                    isSelected -> Color.White.copy(alpha = 0.12f)
                    else -> Color.Transparent
                },
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onFocused)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** Un título de panel, para que todos se vean iguales. */
@Composable
internal fun PanelTitle(text: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
    }
}
