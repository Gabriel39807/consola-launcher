package com.consola.launcher.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R

/**
 * La búsqueda la hace Google (su Asistente sigue instalado aunque saquemos su inicio).
 * Nosotros ponemos la puerta de entrada; reinventar un buscador de películas no tiene sentido.
 */
@Composable
fun SearchTab(onSearch: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        SearchButton(onClick = onSearch)
    }
}

@Composable
private fun SearchButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.06f else 1f, label = "searchScale")
    val content = if (isFocused) Color.Black else Color.White

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(50))
            .background(if (isFocused) Color.White else Color.White.copy(alpha = 0.15f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = content, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.search_button), color = content, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
