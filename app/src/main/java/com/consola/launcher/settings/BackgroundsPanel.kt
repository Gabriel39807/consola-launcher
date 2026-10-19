package com.consola.launcher.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.consola.launcher.R
import com.consola.launcher.upload.UploadStatus
import com.consola.launcher.upload.UploadUiState

private val CardShape = RoundedCornerShape(12.dp)
private val ErrorColor = Color(0xFFFF6B6B)
private val SuccessColor = Color(0xFF2ECC71)
private val ActiveBadge = Color(0xFF3D5AFE)

@Composable
fun BackgroundsPanel(
    backgrounds: List<BackgroundCard>,
    upload: UploadUiState,
    onSelect: (BackgroundChoice) -> Unit,
    onShown: () -> Unit,
    onHidden: () -> Unit,
) {
    val currentOnShown by rememberUpdatedState(onShown)
    val currentOnHidden by rememberUpdatedState(onHidden)
    // El servidor vive SOLO mientras este panel está en pantalla: nada de puertos abiertos de más en tu red.
    DisposableEffect(Unit) {
        currentOnShown()
        onDispose { currentOnHidden() }
    }

    Column(Modifier.fillMaxSize()) {
        UploadCard(upload)
        Spacer(Modifier.height(24.dp))
        PanelTitle(stringResource(R.string.bg_yours))
        BackgroundRow(backgrounds, onSelect)
    }
}

@Composable
private fun UploadCard(upload: UploadUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(130.dp).clip(CardShape).background(Color.White).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            upload.qr?.let { qr ->
                // Sin filtrado: un QR suavizado al agrandarlo se vuelve borroso y la cámara no lo lee.
                Image(qr, contentDescription = upload.url, filterQuality = FilterQuality.None, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(24.dp))
        Column {
            Text(stringResource(R.string.bg_upload_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.bg_upload_steps),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
            )
            upload.url?.let { url ->
                Spacer(Modifier.height(8.dp))
                Text(url, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(8.dp))
            StatusLine(upload.status)
        }
    }
}

@Composable
private fun StatusLine(status: UploadStatus) {
    val (text, color) = when (status) {
        UploadStatus.Off -> stringResource(R.string.bg_status_starting) to Color.White.copy(alpha = 0.6f)
        UploadStatus.Waiting -> stringResource(R.string.bg_status_waiting) to Color.White.copy(alpha = 0.6f)
        UploadStatus.NoNetwork -> stringResource(R.string.bg_status_no_network) to ErrorColor
        is UploadStatus.Receiving -> stringResource(R.string.bg_status_receiving, status.percent) to Color.White
        is UploadStatus.Optimizing -> stringResource(R.string.bg_status_optimizing, status.percent) to Color.White
        is UploadStatus.Done -> stringResource(R.string.bg_status_done) to SuccessColor
        is UploadStatus.Failed -> status.message to ErrorColor
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BackgroundRow(backgrounds: List<BackgroundCard>, onSelect: (BackgroundChoice) -> Unit) {
    LazyRow(
        modifier = Modifier.focusRestorer(),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(backgrounds, key = { it.fileName ?: "aurora" }) { card ->
            BackgroundCardView(card, onClick = { onSelect(card.choice) })
        }
    }
}

@Composable
private fun BackgroundCardView(card: BackgroundCard, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, label = "backgroundScale")

    Column(Modifier.width(160.dp)) {
        Box(
            Modifier
                .size(160.dp, 90.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CardShape)
                .background(Color(0xFF1C2130))
                .then(if (isFocused) Modifier.border(3.dp, Color.White, CardShape) else Modifier)
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        ) {
            when {
                card.thumbnail != null -> Image(
                    bitmap = card.thumbnail,
                    contentDescription = card.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // La aurora no tiene foto: la sugerimos con un degradé estático (barato de dibujar).
                card.fileName == null -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF2E4BFF), Color(0xFF5B2BBF), Color(0xFF00BFA5)))),
                )
            }
            if (card.isActive) {
                Text(
                    text = stringResource(R.string.bg_active),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ActiveBadge)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = card.fileName ?: stringResource(R.string.bg_aurora),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = if (isFocused) 1f else 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
