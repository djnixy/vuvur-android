package com.example.vuvur.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var localActiveApi by remember(state.activeApi) {
        mutableStateOf(state.activeApi)
    }
    var showApiDropdown by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text("API Endpoint", style = MaterialTheme.typography.titleMedium)

            ExposedDropdownMenuBox(
                expanded = showApiDropdown,
                onExpandedChange = { showApiDropdown = !showApiDropdown }
            ) {
                OutlinedTextField(
                    value = localActiveApi,
                    onValueChange = { localActiveApi = it },
                    label = { Text("Active API URL") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showApiDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showApiDropdown,
                    onDismissRequest = { showApiDropdown = false }
                ) {
                    state.apiList.forEach { apiUrl ->
                        DropdownMenuItem(
                            text = { Text(apiUrl) },
                            onClick = {
                                localActiveApi = apiUrl
                                showApiDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ✅ Simplified save button
            Button(onClick = { viewModel.saveSettings(localActiveApi) }) {
                Text("Save")
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Button(onClick = { viewModel.runCacheCleanup() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Clear All Caches & Re-Scan")
            }
        }
    }
}