package com.consola.launcher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

internal val SettingsAccent = Color(0xFF3D5AFE)

/**
 * Una fila de ajustes: ícono, título, descripción y, a la derecha, un interruptor ([switchState]),
 * un valor que cambia con OK ([value]) o nada. Todas las filas de todos los paneles son esta.
 */
@Composable
internal fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    switchState: Boolean? = null,
    value: String? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val content = if (isFocused) Color.Black else Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color.White else Color.White.copy(alpha = 0.06f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = content, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            description?.let { Text(it, color = content.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall) }
        }
        switchState?.let { SwitchIndicator(it) }
        value?.let {
            Spacer(Modifier.width(12.dp))
            Text(it, color = content, style = MaterialTheme.typography.titleSmall)
            // La flechita dice "esto cambia" sin necesidad de explicarlo.
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = content.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun SwitchIndicator(on: Boolean) {
    Box(
        Modifier
            .size(width = 44.dp, height = 24.dp)
            .clip(RoundedCornerShape(50))
            .background(if (on) SettingsAccent else Color.Gray.copy(alpha = 0.6f))
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(Color.White))
    }
}
