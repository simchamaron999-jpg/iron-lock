package com.mistymessenger.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mistymessenger.chat.ui.components.GifItem
import com.mistymessenger.chat.ui.components.GiphyApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class GifViewModel @Inject constructor() : ViewModel() {

    private val giphyApi = Retrofit.Builder()
        .baseUrl("https://api.giphy.com/")
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GiphyApiService::class.java)

    private val apiKey = com.mistymessenger.BuildConfig.GIPHY_API_KEY

    private val _query = MutableStateFlow("")
    private val _gifs = MutableStateFlow<List<GifItem>>(emptyList())
    private val _loading = MutableStateFlow(false)
    val gifs = _gifs.asStateFlow()
    val loading = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(300)
                .collect { q ->
                    _loading.value = true
                    runCatching {
                        if (q.isBlank()) giphyApi.getTrending(apiKey)
                        else giphyApi.search(apiKey, q)
                    }.onSuccess { _gifs.value = it.data }
                        .onFailure { _gifs.value = emptyList() }
                    _loading.value = false
                }
        }
        search("")
    }

    fun search(query: String) { _query.value = query }
}
