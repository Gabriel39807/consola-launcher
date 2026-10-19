package com.consola.launcher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.ui.TvSafeArea

// 5 columnas: (960 dp de ancho − 2×48 de márgenes − 4×16 de separación) / 5 = 160 dp, el tamaño nítido del banner.
private const val Columns = 5

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppsTab(
    apps: List<HomeTile>,
    onFocused: (HomeTile) -> Unit,
    onOpen: (HomeTile) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = TvSafeArea.Horizontal), verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.apps_count, apps.size),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(Columns),
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(
                start = TvSafeArea.Horizontal,
                end = TvSafeArea.Horizontal,
                top = 16.dp,
                bottom = TvSafeArea.Vertical,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(apps, key = { it.key }) { tile ->
                AppTile(
                    tile = tile,
                    onFocused = { onFocused(tile) },
                    onClick = { onOpen(tile) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    focusedScale = 1.1f,
                )
            }
        }
    }
}
