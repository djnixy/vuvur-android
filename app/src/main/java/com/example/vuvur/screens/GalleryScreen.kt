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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        "date_asc" to "Older first",
        "date_desc" to "Newest first"
    )
    val focusManager = LocalFocusManager.current

    var showDeleteDialog by remember { mutableStateOf<Int?>(null) }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to move this file to the recycle bin?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog?.let { viewModel.deleteMediaItem(it) }
                        showDeleteDialog = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search using tags") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = {
                        IconButton(onClick = {
                            viewModel.applySearch(searchQuery)
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search by tags")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.applySearch(searchQuery)
                            focusManager.clearFocus()
                        }
                    )
                )

                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Sort Options")
                }
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    sortOptions.forEach { (key, value) ->
                        DropdownMenuItem(
                            text = { Text(value) },
                            onClick = {
                                viewModel.applySort(key)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                // ✅ Refactored logic to correctly handle UI states and fix the warning
                when (val currentState = state) {
                    is GalleryUiState.Success -> {
                        if (currentState.files.isEmpty()) {
                            Text("No media found.")
                        } else {
                            GalleryGrid(
                                files = currentState.files,
                                activeApiUrl = currentState.activeApiUrl,
                                onScrolledToEnd = {
                                    viewModel.loadPage(currentState.currentPage + 1)
                                },
                                onImageClick = onImageClick,
                                onDeleteClick = { fileId -> showDeleteDialog = fileId }
                            )
                        }
                    }
                    is GalleryUiState.Error -> Text("Failed to load: ${currentState.message}")
                    is GalleryUiState.Scanning -> Text("Scanning Library: ${currentState.progress} / ${currentState.total}")
                    is GalleryUiState.Loading -> {
                        // Show the centered indicator only on initial load when the screen is empty
                        CircularProgressIndicator()
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
    onImageClick: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit
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
                onClick = { onImageClick(index) },
                onDeleteClick = { onDeleteClick(file.id) }
            )
        }
    }
}

@Composable
fun MediaThumbnail(
    file: MediaFile,
    activeApiUrl: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val thumbnailUrl = "$activeApiUrl/api/thumbnails/${file.id}"
    val aspectRatio = if (file.height > 0 && file.width > 0) {
        file.width.toFloat() / file.height.toFloat()
    } else {
        1.0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(aspectRatio)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(thumbnailUrl).crossfade(true)
                .build(),
            contentDescription = file.path,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}