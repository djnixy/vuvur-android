package com.example.vuvur.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.vuvur.GalleryUiState
import com.example.vuvur.MediaFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MediaViewModel,
    onImageClick: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing = state is GalleryUiState.Loading

    var searchQuery by remember { mutableStateOf("") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val sortOptions = mapOf(
        "random" to "Random",
        "date_asc" to "Date Ascending",
        "date_desc" to "Date Descending"
    )
    var selectedSort by remember { mutableStateOf("random") }

    // ✅ Get the focus manager to control the keyboard
    val focusManager = LocalFocusManager.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search using tags") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        viewModel.applySearch(searchQuery)
                        // Hide keyboard on search
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Search by tags")
                    }
                },
                // ✅ Add a search button to the keyboard
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                // ✅ Trigger search when the keyboard search button is pressed
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.applySearch(searchQuery)
                        focusManager.clearFocus()
                    }
                )
            )

            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Sort Options")
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    sortOptions.forEach { (key, value) ->
                        DropdownMenuItem(
                            text = { Text(value) },
                            onClick = {
                                selectedSort = key
                                sortMenuExpanded = false
                            }
                        )
                    }
                    // ✅ Move "Apply Sort" button inside the dropdown menu
                    Box(modifier = Modifier.padding(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.applySort(selectedSort)
                                sortMenuExpanded = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Sort")
                        }
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                when (val currentState = state) {
                    is GalleryUiState.Loading -> {
                        if ((viewModel.uiState.value as? GalleryUiState.Success)?.files.isNullOrEmpty()) {
                            CircularProgressIndicator()
                        }
                    }
                    is GalleryUiState.Error -> Text("Failed to load: ${currentState.message}")
                    is GalleryUiState.Scanning -> Text("Scanning Library: ${currentState.progress} / ${currentState.total}")
                    is GalleryUiState.Success -> {
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
}

@Composable
fun GalleryGrid(
    files: List<MediaFile>,
    activeApiUrl: String,
    onScrolledToEnd: () -> Unit,
    onImageClick: (Int) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        itemsIndexed(files, key = { _, file -> file.id }) { index, file ->
            if (index >= files.size - 10) {
                LaunchedEffect(Unit) {
                    onScrolledToEnd()
                }
            }
            MediaThumbnail(
                file = file,
                activeApiUrl = activeApiUrl,
                onClick = { onImageClick(index) }
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
    val thumbnailUrl = "$activeApiUrl/api/thumbnails/${file.id}"
    val aspectRatio = if (file.height > 0 && file.width > 0) {
        file.width.toFloat() / file.height.toFloat()
    } else { 1.0f }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(thumbnailUrl).crossfade(true).build(),
        contentDescription = file.path,
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(aspectRatio)
            .clickable { onClick() },
        contentScale = ContentScale.Crop
    )
}