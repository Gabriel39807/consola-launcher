package com.consola.launcher.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.apps.AppArtwork
import com.consola.launcher.upload.UploadUiState
import androidx.compose.ui.graphics.FilterQuality

private val CheckedColor = Color(0xFF3D5AFE)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppsPanel(
    rows: List<AppSettingRow>,
    upload: UploadUiState,
    onToggleFavorite: (String) -> Unit,
    onToggleHidden: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        PanelTitle(stringResource(R.string.settings_apps))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Escribir nombres con el control es un martirio: los nombres y portadas se cambian desde el celular.
            upload.qr?.let { qr ->
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(4.dp)) {
                    Image(qr, contentDescription = upload.url, filterQuality = FilterQuality.None, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(16.dp))
            }
            Column {
                Text(
                    text = stringResource(R.string.apps_phone_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    text = stringResource(R.string.apps_settings_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
        LazyColumn(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rows, key = { it.tile.key }) { row ->
                AppRowItem(
                    row = row,
                    onToggleFavorite = { onToggleFavorite(row.tile.packageName) },
                    onToggleHidden = { onToggleHidden(row.tile.packageName) },
                )
            }
        }
    }
}

@Composable
private fun AppRowItem(row: AppSettingRow, onToggleFavorite: () -> Unit, onToggleHidden: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(80.dp, 45.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1C2130)), contentAlignment = Alignment.Center) {
            when (val artwork = row.tile.artwork) {
                is AppArtwork.Banner -> Image(artwork.image, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                is AppArtwork.Icon -> Image(artwork.image, null, modifier = Modifier.size(32.dp))
                AppArtwork.None -> Unit
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = row.tile.label,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        ToggleChip(stringResource(R.string.apps_favorite), Icons.Default.Star, row.tile.isFavorite, onToggleFavorite)
        Spacer(Modifier.width(8.dp))
        ToggleChip(stringResource(R.string.apps_hidden), Icons.Default.Check, row.isHidden, onToggleHidden)
    }
}

@Composable
private fun ToggleChip(label: String, icon: ImageVector, checked: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val content = if (isFocused) Color.Black else Color.White.copy(alpha = if (checked) 1f else 0.6f)
    val background = when {
        isFocused -> Color.White
        checked -> CheckedColor
        else -> Color.White.copy(alpha = 0.08f)
    }

    Row(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // El ícono solo aparece si está marcada: así se lee de un vistazo, sin depender del color.
        if (checked) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = content, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}
