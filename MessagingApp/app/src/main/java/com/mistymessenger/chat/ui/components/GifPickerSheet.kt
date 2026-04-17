package com.mistymessenger.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyApiService {
    @GET("v1/gifs/trending")
    suspend fun getTrending(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 25,
        @Query("rating") rating: String = "g"
    ): GiphyResponse

    @GET("v1/gifs/search")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 25,
        @Query("rating") rating: String = "g"
    ): GiphyResponse
}

data class GiphyResponse(val data: List<GifItem>)
data class GifItem(val id: String, val images: GifImages)
data class GifImages(
    val fixed_height_small: GifImage,
    val original: GifImage
)
data class GifImage(val url: String, val width: String = "0", val height: String = "0")

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun GifPickerSheet(
    onDismiss: () -> Unit,
    onGifSelected: (String) -> Unit,   // returns gif URL
    gifs: List<GifItem>,
    isLoading: Boolean,
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        snapshotFlow { query }
            .debounce(300)
            .collect { onSearch(it) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxHeight(0.75f)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search GIFs") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(gifs, key = { it.id }) { gif ->
                        AsyncImage(
                            model = gif.images.fixed_height_small.url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clickable { onGifSelected(gif.images.original.url) }
                        )
                    }
                }
            }
        }
    }
}
