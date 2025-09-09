package com.example.vuvur.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.vuvur.MediaFile
import kotlinx.coroutines.launch

@Composable
fun MediaSlide(
    file: MediaFile,
    activeApiUrl: String,
    isZoomed: Boolean,
    onZoomToggle: () -> Unit,
    zoomLevel: Float = 2.5f
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // This effect now ALSO watches for changes to the scrollbar max values.
    // This fixes the race condition.
    LaunchedEffect(isZoomed, verticalScrollState.maxValue, horizontalScrollState.maxValue) {
        if (isZoomed) {
            // It will run once when isZoomed=true (maxes are 0),
            // and then run AGAIN when the layout calculates the new max values.
            // We scroll to the center only when the max values are available.
            val hMax = horizontalScrollState.maxValue
            val vMax = verticalScrollState.maxValue
            if (hMax > 0 || vMax > 0) {
                scope.launch {
                    horizontalScrollState.scrollTo(hMax / 2)
                    verticalScrollState.scrollTo(vMax / 2)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (file.type == "image") onZoomToggle()
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isZoomed) Modifier
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (file.type == "image") {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl(file, activeApiUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = file.path,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = if (isZoomed) zoomLevel else 1f,
                            scaleY = if (isZoomed) zoomLevel else 1f
                        )
                        .fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl(file, activeApiUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = file.path,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

private fun imageUrl(file: MediaFile, activeApiUrl: String): String {
    return "$activeApiUrl/api/preview/${file.path}"
}