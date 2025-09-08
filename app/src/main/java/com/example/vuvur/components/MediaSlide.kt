package com.example.vuvur.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.vuvur.MediaFile

@Composable
fun MediaSlide(
    file: MediaFile,
    activeApiUrl: String,
    isZoomed: Boolean,
    onZoomToggle: () -> Unit,
    zoomLevel: Float = 2.5f // We can get this from settings later
) {
    // This component no longer needs complex state. It just receives zoom state.

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onZoomToggle() // Always report a double-tap
                    }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl(file, activeApiUrl))
                .crossfade(true)
                .build(),
            contentDescription = file.path,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = if (isZoomed) zoomLevel else 1f,
                    scaleY = if (isZoomed) zoomLevel else 1f,
                    translationX = 0f,
                    translationY = 0f
                ),
            contentScale = ContentScale.Fit
        )
    }
}

private fun imageUrl(file: MediaFile, activeApiUrl: String): String {
    return "$activeApiUrl/api/preview/${file.path}"
}