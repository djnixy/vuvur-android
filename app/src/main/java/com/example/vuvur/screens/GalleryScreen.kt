package com.example.vuvur.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.vuvur.GalleryUiState
import com.example.vuvur.GroupInfo
import com.example.vuvur.MediaFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MediaViewModel,
    onImageClick: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    // Determine refreshing state based on Loading state *without* an initial API URL (meaning manual refresh)
    val isRefreshing = state is GalleryUiState.Loading && (state as GalleryUiState.Loading).apiUrl == null

    var searchQuery by remember { mutableStateOf("") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val sortOptions = mapOf(
        "random" to "Random",
        "date_asc" to "Older first",
        "date_desc" to "Newest first"
    )
    val focusManager = LocalFocusManager.current
    var showDeleteDialog by remember { mutableStateOf<Int?>(null) }

    // Dialog for Delete Confirmation
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
        // Search and Sort Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
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

            // Dropdown Menu for Sorting (aligned to the end)
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

        // Quick Access Buttons Row
        // Only show groups row if state is Success and groups are not empty
        if (state is GalleryUiState.Success && (state as GalleryUiState.Success).groups.isNotEmpty()) {
            val successState = state as GalleryUiState.Success
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All" Button
                item {
                    QuickAccessButton(
                        text = "All",
                        isSelected = successState.selectedGroupTag == null,
                        onClick = { viewModel.applyGroupFilter(null) }
                    )
                }
                // Group Buttons
                items(successState.groups, key = { it.groupTag }) { group ->
                    QuickAccessButton(
                        text = "${group.groupTag} (${group.count})",
                        isSelected = successState.selectedGroupTag == group.groupTag,
                        onClick = { viewModel.applyGroupFilter(group.groupTag) }
                    )
                }
            }
        }

        // PullToRefreshBox now wraps the main content area unconditionally
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize() // Make it fill remaining space
        ) {
            // Content Box within PullToRefresh - REMOVE verticalScroll
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
                // Removed .verticalScroll(rememberScrollState())
            ) {
                when (val currentState = state) {
                    is GalleryUiState.Success -> {
                        if (currentState.files.isEmpty()) {
                            // Centered message within the Box, PTR should still work
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center, // Center vertically too
                                modifier = Modifier.fillMaxSize().padding(16.dp) // Fill size for centering
                            ) {
                                Text("No media found.")
                                Spacer(Modifier.height(8.dp))
                                Text("(Pull down to refresh)")
                            }
                        } else {
                            // Let the LazyVerticalStaggeredGrid handle its own scrolling
                            GalleryGrid(
                                files = currentState.files,
                                activeApiUrl = currentState.activeApiUrl,
                                onScrolledToEnd = {
                                    if (!currentState.isLoadingNextPage && currentState.currentPage < currentState.totalPages) {
                                        viewModel.loadPage(currentState.currentPage + 1)
                                    }
                                },
                                onImageClick = onImageClick,
                                onDeleteClick = { fileId -> showDeleteDialog = fileId }
                            )
                            if (currentState.isLoadingNextPage) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
                            }
                        }
                    }
                    is GalleryUiState.Error -> {
                        // Centered message within the Box, PTR should still work
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center, // Center vertically too
                            modifier = Modifier.fillMaxSize().padding(16.dp) // Fill size for centering
                        ) {
                            Text("Failed to load: ${currentState.message}")
                            Spacer(Modifier.height(8.dp))
                            Text("(Pull down to refresh)")
                        }
                    }
                    is GalleryUiState.Scanning -> {
                        // Centered message within the Box, PTR should still work
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center, // Center vertically too
                            modifier = Modifier.fillMaxSize().padding(16.dp) // Fill size for centering
                        ) {
                            Text("Scanning Library...", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Please wait, this may take a few minutes for a large collection.",
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("${currentState.progress} / ${currentState.total} files scanned")
                            Spacer(Modifier.height(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                    is GalleryUiState.Loading -> {
                        // Centered spinner within the Box
                        if (currentState.apiUrl != null) {
                            CircularProgressIndicator()
                        }
                        // Manual refresh indicator is handled by PTR
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (isSelected) {
        ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    ElevatedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp), // Pill shape
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), // Adjust padding
        colors = colors
    ) {
        Text(text)
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
        modifier = Modifier.fillMaxSize(), // Add fillMaxSize here
        contentPadding = PaddingValues(4.dp),
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(files, key = { _, file -> file.id }) { index, file ->
            // Trigger load more when nearing the end (e.g., 10 items away)
            if (index >= files.size - 10) {
                LaunchedEffect(Unit) { // Use Unit key to trigger only once when condition met
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
        1.0f // Default aspect ratio if dimensions are invalid
    }

    Box(
        modifier = Modifier
            .fillMaxWidth() // Use fillMaxWidth for staggered grid items
            .aspectRatio(aspectRatio)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = file.path,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Crop maintains aspect ratio better here
        )
        // Smaller delete button
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp) // Add some padding
                .size(24.dp) // Make button smaller
                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape) // Background for visibility
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White, // White icon for contrast
                modifier = Modifier.size(16.dp) // Make icon smaller
            )
        }
    }
}