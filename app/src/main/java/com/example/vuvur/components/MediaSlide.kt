package com.example.vuvur.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.vuvur.MediaFile
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.DisposableEffect

@Composable
fun MediaSlide(
    file: MediaFile,
    activeApiUrl: String,
    onNextImage: () -> Unit,
    onPreviousImage: () -> Unit,
    allowSwipeNavigation: Boolean = true // true for Random/Main pages, false for Single mode
) {
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Reset zoom/pan when file changes
    LaunchedEffect(file) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        fun maxOffsets(currentScale: Float): Pair<Float, Float> {
            val maxX = (containerWidth * (currentScale - 1f)) / 2f
            val maxY = (containerHeight * (currentScale - 1f)) / 2f
            return Pair(maxX.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
        }

        LaunchedEffect(scale) {
            if (scale > 1f) {
                val (mx, my) = maxOffsets(scale)
                offsetX = offsetX.coerceIn(-mx, mx)
                offsetY = offsetY.coerceIn(-my, my)
            } else {
                offsetX = 0f
                offsetY = 0f
            }
        }

        // Only attach drag gestures for pan when zoomed in
        val dragModifier = if (scale > 1f) {
            Modifier.pointerInput(scale) {
                detectDragGestures { change, dragAmount ->
                    val (dx, dy) = dragAmount
                    val (mx, my) = maxOffsets(scale)
                    offsetX = (offsetX + dx).coerceIn(-mx, mx)
                    offsetY = (offsetY + dy).coerceIn(-my, my)
                    change.consume() // only consume when zoomed in
                }
            }
        } else Modifier

        // Gesture handling
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(scale) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (file.type == "image") {
                                if (scale <= 1f) {
                                    val targetScale = 2f
                                    val tx = (containerWidth / 2f - tapOffset.x) * (targetScale - 1f)
                                    val ty = (containerHeight / 2f - tapOffset.y) * (targetScale - 1f)
                                    scale = targetScale
                                    val (mx, my) = maxOffsets(scale)
                                    offsetX = tx.coerceIn(-mx, mx)
                                    offsetY = ty.coerceIn(-my, my)
                                } else {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                    )
                }
                .then(dragModifier) // attach pan only when zoomed in
        ) {
            if (file.type == "image") {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        // ✅ Use the /api/stream/ endpoint for full-size images
                        .data("$activeApiUrl/api/stream/${file.id}")
                        .crossfade(true)
                        .build(),
                    contentDescription = file.path,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        val mediaItem =
                            MediaItem.fromUri("$activeApiUrl/api/stream/${file.id}")
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }
                }
                DisposableEffect(Unit) {
                    onDispose { exoPlayer.release() }
                }
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}