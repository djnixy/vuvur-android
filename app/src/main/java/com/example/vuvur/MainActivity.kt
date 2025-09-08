package com.example.vuvur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.vuvur.ui.theme.VuvurTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VuvurTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GalleryScreen()
                }
            }
        }
    }
}

@Composable
fun GalleryScreen(viewModel: GalleryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                is GalleryUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is GalleryUiState.Error -> {
                    Text("Failed to load: ${currentState.message}")
                }
                is GalleryUiState.Scanning -> {
                    Text("Scanning Library: ${currentState.progress} / ${currentState.total}")
                }
                is GalleryUiState.Success -> {
                    GalleryGrid(
                        files = currentState.files,
                        onScrolledToEnd = {
                            viewModel.loadPage(currentState.currentPage + 1)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryGrid(files: List<MediaFile>, onScrolledToEnd: () -> Unit) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(150.dp), // Creates adaptive columns ~150dp wide
        contentPadding = PaddingValues(4.dp)
    ) {
        itemsIndexed(files) { index, file ->
            // Check if we are near the end of the list
            if (index >= files.size - 5) {
                LaunchedEffect(Unit) {
                    onScrolledToEnd()
                }
            }

            MediaThumbnail(file = file)
        }
    }
}

@Composable
fun MediaThumbnail(file: MediaFile) {
    // Build the full URL to the thumbnail
    val thumbnailUrl = "${ApiClient.API_BASE_URL}/api/thumbnail/${file.path}"

    // Calculate aspect ratio to prevent layout shifting (just like in React!)
    val aspectRatio = if (file.height > 0 && file.width > 0) {
        file.width.toFloat() / file.height.toFloat()
    } else {
        1.0f // Default to square if dimensions are 0 (e.g., for videos)
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(thumbnailUrl)
            .crossfade(true)
            .build(),
        contentDescription = file.path,
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(aspectRatio),
        contentScale = ContentScale.Crop
    )
}