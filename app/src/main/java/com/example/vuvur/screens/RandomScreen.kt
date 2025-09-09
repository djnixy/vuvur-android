package com.example.vuvur.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vuvur.Screen
import com.example.vuvur.components.MediaSlide

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RandomScreen(
    viewModel: RandomViewModel = viewModel(),
    navController: NavController
) {
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeApiUrl = viewModel.getActiveApiUrl()
    val zoomLevel by viewModel.zoomLevel.collectAsState()

    var zoomedPageIndex by remember { mutableStateOf(-1) }
    val isPagerScrollEnabled = zoomedPageIndex == -1

    Box(modifier = Modifier.fillMaxSize()) {
        if (files.isEmpty() && isLoading) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
            return
        }

        if (files.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("No media found.")
            }
            return
        }

        val pagerState = rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f,
            pageCount = { files.size }
        )

        LaunchedEffect(pagerState.isScrollInProgress) {
            if (pagerState.isScrollInProgress) {
                zoomedPageIndex = -1
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage >= files.size - 3) {
                viewModel.loadNextRandomImages(5)
            }
        }

        VerticalPager(
            state = pagerState,
            userScrollEnabled = isPagerScrollEnabled,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val file = files[pageIndex]
            MediaSlide(
                file = file,
                activeApiUrl = activeApiUrl,
                isZoomed = (zoomedPageIndex == pageIndex),
                onZoomToggle = {
                    zoomedPageIndex = if (zoomedPageIndex == pageIndex) -1 else pageIndex
                },
                zoomLevel = zoomLevel.toFloat()
            )
        }

        IconButton(
            onClick = { navController.navigate(Screen.Gallery.route) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close Random Mode")
        }
    }
}