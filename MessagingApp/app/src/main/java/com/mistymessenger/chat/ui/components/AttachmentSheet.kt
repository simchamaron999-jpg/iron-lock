package com.mistymessenger.chat.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class AttachmentOption(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    onDismiss: () -> Unit,
    onImageSelected: (Uri, Boolean) -> Unit,     // uri, fullRes
    onVideoSelected: (Uri, Boolean) -> Unit,     // uri, fullRes
    onDocumentSelected: (Uri) -> Unit,
    onCameraCapture: (Uri) -> Unit,
    onGifPick: () -> Unit,
    onStickerPick: () -> Unit,
    onLocationShare: () -> Unit,
    onContactShare: () -> Unit,
    fullResEnabled: Boolean,
    onToggleFullRes: (Boolean) -> Unit
) {
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onImageSelected(it, fullResEnabled) } }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onVideoSelected(it, fullResEnabled) } }

    val docPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onDocumentSelected(it) } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(
                "Share",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Full res toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.HighQuality, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Full resolution", style = MaterialTheme.typography.bodyMedium)
                    Text("Skip compression (larger files)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = fullResEnabled, onCheckedChange = onToggleFullRes)
            }

            HorizontalDivider(Modifier.padding(bottom = 16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachTile(
                    label = "Image",
                    icon = Icons.Default.Image,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                AttachTile(
                    label = "Video",
                    icon = Icons.Default.VideoLibrary,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                }
                AttachTile(
                    label = "Camera",
                    icon = Icons.Default.CameraAlt,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) { onCameraCapture(Uri.EMPTY) }
                AttachTile(
                    label = "Document",
                    icon = Icons.Default.Description,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) { docPicker.launch("*/*") }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachTile("GIF", Icons.Default.Gif, MaterialTheme.colorScheme.errorContainer) { onGifPick() }
                AttachTile("Sticker", Icons.Default.EmojiEmotions, MaterialTheme.colorScheme.primaryContainer) { onStickerPick() }
                AttachTile("Location", Icons.Default.LocationOn, MaterialTheme.colorScheme.secondaryContainer) { onLocationShare() }
                AttachTile("Contact", Icons.Default.Contacts, MaterialTheme.colorScheme.tertiaryContainer) { onContactShare() }
            }
        }
    }
}

@Composable
private fun AttachTile(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
