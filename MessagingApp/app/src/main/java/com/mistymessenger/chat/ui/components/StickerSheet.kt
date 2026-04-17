package com.mistymessenger.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class StickerPack(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val stickers: List<Sticker>
)

data class Sticker(val id: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerSheet(
    onDismiss: () -> Unit,
    onStickerSelected: (String) -> Unit,   // sticker URL
    packs: List<StickerPack>,
    onDownloadMorePacks: () -> Unit
) {
    var selectedPackIdx by remember { mutableIntStateOf(0) }
    val currentStickers = packs.getOrNull(selectedPackIdx)?.stickers ?: emptyList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxHeight(0.6f)) {
            if (packs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EmojiEmotions, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("No sticker packs", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onDownloadMorePacks) { Text("Browse Sticker Packs") }
                    }
                }
            } else {
                // Pack selector tabs
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(packs) { idx, pack ->
                        FilterChip(
                            selected = idx == selectedPackIdx,
                            onClick = { selectedPackIdx = idx },
                            label = { Text(pack.name, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                AsyncImage(
                                    model = pack.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onDownloadMorePacks,
                            label = { Text("More", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                HorizontalDivider()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(currentStickers, key = { it.id }) { sticker ->
                        AsyncImage(
                            model = sticker.url,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(72.dp)
                                .clickable { onStickerSelected(sticker.url) }
                        )
                    }
                }
            }
        }
    }
}
