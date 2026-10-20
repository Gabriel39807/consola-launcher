package com.consola.launcher.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R

private val PreviewShape = RoundedCornerShape(12.dp)
private val PreviewBackground = Color(0xFF141A2B)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StylePanel(current: HomeLayout, onSelect: (HomeLayout) -> Unit) {
    // Dos columnas con scroll: entran las vistas previas a buen tamaño aunque sumemos más estilos.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.focusRestorer(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 16.dp, top = 4.dp, start = 4.dp, end = 4.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { PanelTitle(stringResource(R.string.settings_style)) }
        items(HomeLayout.entries) { layout -> StyleCard(layout, layout == current, onClick = { onSelect(layout) }) }
    }
}

@Composable
private fun StyleCard(layout: HomeLayout, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "styleScale")

    Column(Modifier.width(260.dp)) {
        Box(
            Modifier
                .size(260.dp, 120.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(PreviewShape)
                .background(PreviewBackground)
                .then(
                    when {
                        isFocused -> Modifier.border(3.dp, Color.White, PreviewShape)
                        isSelected -> Modifier.border(2.dp, SettingsAccent, PreviewShape)
                        else -> Modifier
                    },
                )
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        ) {
            // Un dibujito del estilo: se entiende más rápido que cualquier descripción.
            Canvas(Modifier.fillMaxSize().padding(12.dp)) { drawSchematic(layout, if (isSelected) SettingsAccent else Color.White) }
            if (isSelected) {
                Text(
                    text = stringResource(R.string.bg_active),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(SettingsAccent)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(layout.title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(layout.description), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
    }
}

private val HomeLayout.title: Int
    get() = when (this) {
        HomeLayout.Cinema -> R.string.layout_cinema
        HomeLayout.Ps5 -> R.string.layout_ps5
        HomeLayout.Xbox -> R.string.layout_xbox
        HomeLayout.Minimal -> R.string.layout_minimal
        HomeLayout.Rows -> R.string.layout_rows
    }

private val HomeLayout.description: Int
    get() = when (this) {
        HomeLayout.Cinema -> R.string.layout_cinema_desc
        HomeLayout.Ps5 -> R.string.layout_ps5_desc
        HomeLayout.Xbox -> R.string.layout_xbox_desc
        HomeLayout.Minimal -> R.string.layout_minimal_desc
        HomeLayout.Rows -> R.string.layout_rows_desc
    }

/** Bloques que imitan cada estilo, en proporciones de la pantalla real (las medidas son fracciones). */
private fun DrawScope.drawSchematic(layout: HomeLayout, accent: Color) {
    val strong = Color.White.copy(alpha = 0.85f)
    val soft = Color.White.copy(alpha = 0.3f)
    fun block(x: Float, y: Float, w: Float, h: Float, color: Color) = drawRoundRect(
        color = color,
        topLeft = Offset(size.width * x, size.height * y),
        size = Size(size.width * w, size.height * h),
        cornerRadius = CornerRadius(6f),
    )
    when (layout) {
        HomeLayout.Cinema -> {
            block(0f, 0.08f, 0.45f, 0.16f, strong)
            block(0.62f, 0.05f, 0.38f, 0.32f, soft)
            for (i in 0 until 4) block(i * 0.25f, 0.68f, 0.21f, 0.28f, if (i == 0) accent else soft)
        }
        HomeLayout.Ps5 -> {
            for (i in 0 until 6) block(i * 0.15f, 0f, if (i == 0) 0.15f else 0.12f, if (i == 0) 0.26f else 0.2f, if (i == 0) accent else soft)
            block(0f, 0.38f, 0.5f, 0.16f, strong)
            block(0f, 0.72f, 0.42f, 0.26f, soft)
        }
        HomeLayout.Xbox -> {
            block(0f, 0f, 0.36f, 0.62f, accent)
            for (row in 0 until 2) for (col in 0 until 3) block(0.4f + col * 0.2f, row * 0.32f, 0.18f, 0.29f, soft)
            block(0f, 0.76f, 0.35f, 0.14f, strong)
        }
        HomeLayout.Minimal -> {
            block(0.3f, 0.22f, 0.4f, 0.26f, strong)
            block(0.38f, 0.52f, 0.24f, 0.07f, soft)
            for (i in 0 until 4) block(0.2f + i * 0.155f, 0.78f, 0.13f, 0.2f, if (i == 0) accent else soft)
        }
        HomeLayout.Rows -> {
            block(0f, 0f, 0.4f, 0.12f, strong)
            for (row in 0 until 3) for (i in 0 until 5) {
                block(i * 0.2f, 0.2f + row * 0.28f, 0.17f, 0.22f, if (row == 0 && i == 0) accent else soft)
            }
        }
    }
}
