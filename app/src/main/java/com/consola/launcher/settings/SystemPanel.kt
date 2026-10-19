package com.consola.launcher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.ConsolaApp
import com.consola.launcher.R
import com.consola.launcher.home.SystemActions

private val SwitchOn = Color(0xFF3D5AFE)

@Composable
fun SystemPanel() {
    val context = LocalContext.current
    val screensaver = remember { (context.applicationContext as ConsolaApp).graph.screensaver }
    var screensaverActive by remember { mutableStateOf(screensaver.isActive()) }
    var canChangeScreensaver by remember { mutableStateOf(screensaver.canChange()) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

    // Si alguien corrió el script de la PC mientras estabas en otra app, al volver ya se ve habilitado.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        screensaverActive = screensaver.isActive()
        canChangeScreensaver = screensaver.canChange()
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PanelTitle(stringResource(R.string.settings_system))
        // Sin el inicio de Google, esta es la única puerta a los ajustes de Android: tiene que estar SIEMPRE.
        ActionRow(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.tv_settings),
            description = stringResource(R.string.tv_settings_desc),
            onClick = { SystemActions.openTvSettings(context) },
        )
        ActionRow(
            icon = Icons.Default.Star,
            title = stringResource(R.string.screensaver_title),
            description = stringResource(
                when {
                    !canChangeScreensaver -> R.string.screensaver_needs_pc
                    screensaverActive -> R.string.screensaver_on
                    else -> R.string.screensaver_off
                },
            ),
            switchState = if (canChangeScreensaver) screensaverActive else null,
            onClick = {
                if (canChangeScreensaver) {
                    screensaver.setActive(!screensaverActive)
                    screensaverActive = screensaver.isActive()
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.about_version, version),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f),
        )
    }
}

/** Una fila de ajustes. Con [switchState] muestra un interruptor; null = sin interruptor. */
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    switchState: Boolean? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val content = if (isFocused) Color.Black else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color.White else Color.White.copy(alpha = 0.06f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = content, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(description, color = content.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        }
        switchState?.let { on -> Switch(on) }
    }
}

@Composable
private fun Switch(on: Boolean) {
    Box(
        Modifier
            .size(width = 44.dp, height = 24.dp)
            .clip(RoundedCornerShape(50))
            .background(if (on) SwitchOn else Color.Gray.copy(alpha = 0.6f))
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(Color.White))
    }
}
