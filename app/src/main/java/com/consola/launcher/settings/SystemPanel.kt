package com.consola.launcher.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.ConsolaApp
import com.consola.launcher.R
import com.consola.launcher.home.SystemActions

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
        SettingsRow(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.tv_settings),
            description = stringResource(R.string.tv_settings_desc),
            onClick = { SystemActions.openTvSettings(context) },
        )
        SettingsRow(
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
