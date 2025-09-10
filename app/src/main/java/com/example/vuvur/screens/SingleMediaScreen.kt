package com.example.vuvur.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.navigation.NavController
import com.example.vuvur.GalleryUiState
//import com.example.vuvur.MediaViewModel
import com.example.vuvur.components.MediaSlide

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleMediaScreen(
    viewModel: MediaViewModel,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsState()
    var zoomedPageIndex by remember { mutableStateOf(-1) }
    val isPagerScrollEnabled = zoomedPageIndex == -1
    val zoomLevel = viewModel.getZoomLevel()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is GalleryUiState.Success -> {
                if (currentState.files.isEmpty()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("No media found.")
                    }
                    return
                }

                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { currentState.files.size }
                )

                LaunchedEffect(pagerState.isScrollInProgress) {
                    if (pagerState.isScrollInProgress) {
                        zoomedPageIndex = -1
                    }
                }

                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage >= currentState.files.size - 3) {
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
                        isZoomed = (zoomedPageIndex == pageIndex),
                        onZoomToggle = {
                            zoomedPageIndex = if (zoomedPageIndex == pageIndex) -1 else pageIndex
                        },
                        zoomLevel = zoomLevel
                    )
                }
            }
            else -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}