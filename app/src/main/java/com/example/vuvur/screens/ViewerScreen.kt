package com.example.vuvur.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vuvur.GalleryUiState
import com.example.vuvur.screens.MediaViewModel
import com.example.vuvur.components.MediaSlide

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    viewModel: MediaViewModel,
    startIndex: Int,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsState()
    var zoomedPageIndex by remember { mutableStateOf(-1) }
    val isPagerScrollEnabled = zoomedPageIndex == -1
    val zoomLevel = viewModel.getZoomLevel()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is GalleryUiState.Success -> {
                val pagerState = rememberPagerState(
                    initialPage = startIndex,
                    pageCount = { currentState.files.size }
                )

                LaunchedEffect(pagerState.isScrollInProgress) {
                    if (pagerState.isScrollInProgress) {
                        zoomedPageIndex = -1
                    }
                }

                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage >= currentState.files.size - 10) {
                        viewModel.loadPage(currentState.currentPage + 1)
                    }
                }

                VerticalPager(
                    state = pagerState,
                    userScrollEnabled = isPagerScrollEnabled,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val file = currentState.files[pageIndex]
                    MediaSlide(
                        file = file,
                        activeApiUrl = currentState.activeApiUrl,
                        onNextImage = { /* logic to go to next image */ },
                        onPreviousImage = { /* logic to go to previous image */ },
                        allowSwipeNavigation = true
                    )
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading viewer...")
                }
            }
        }

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close Viewer")
        }
    }
}