package com.example.vuvur.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.vuvur.GalleryUiState
import com.example.vuvur.MediaFile
import com.example.vuvur.screens.MediaViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun GalleryScreen(
    viewModel: MediaViewModel,
    onImageClick: (MediaFile) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is GalleryUiState.Loading -> CircularProgressIndicator()

            is GalleryUiState.Error -> Text("Failed to load: ${currentState.message}")

            is GalleryUiState.Scanning -> Text(
                "Scanning Library: ${currentState.progress} / ${currentState.total}"
            )

            is GalleryUiState.Success -> {
                // Refreshing when state is Loading
                val swipeRefreshState =
                    rememberSwipeRefreshState(isRefreshing = state is GalleryUiState.Loading)

                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refresh() }
                ) {
                    GalleryGrid(
                        files = currentState.files,
                        activeApiUrl = currentState.activeApiUrl,
                        onScrolledToEnd = {
                            viewModel.loadPage(currentState.currentPage + 1)
                        },
                        onImageClick = onImageClick
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryGrid(
    files: List<MediaFile>,
    activeApiUrl: String,
    onScrolledToEnd: () -> Unit,
    onImageClick: (MediaFile) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        itemsIndexed(
            files,
            key = { _, file -> file.path } // must be unique
        ) { index, file ->

            // Trigger pagination when last item is reached
            if (index == files.lastIndex) {
                LaunchedEffect(index) {
                    onScrolledToEnd()
                }
            }

            MediaThumbnail(
                file = file,
                activeApiUrl = activeApiUrl,
                onClick = { onImageClick(file) }
            )
        }
    }
}

@Composable
fun MediaThumbnail(
    file: MediaFile,
    activeApiUrl: String,
    onClick: () -> Unit
) {
    val thumbnailUrl = "$activeApiUrl/api/thumbnail/${file.path}"
    val aspectRatio = if (file.height > 0 && file.width > 0) {
        file.width.toFloat() / file.height.toFloat()
    } else {
        1.0f
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(thumbnailUrl)
            .crossfade(true)
            .build(),
        contentDescription = file.path,
        modifier = Modifier
            .aspectRatio(aspectRatio)
            .clickable { onClick() },
        contentScale = ContentScale.Crop
    )
}
