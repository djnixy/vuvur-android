package com.example.vuvur.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    val lockedKeys = state.lockedKeys

    var localSettings by remember(settings) {
        mutableStateOf(settings)
    }
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
        if (state.isLoading || localSettings == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    modifier = Modifier.fillMaxWidth().menuAnchor()
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

            Text("Performance Settings", style = MaterialTheme.typography.titleMedium)
            SettingTextField(
                label = "Gallery images per scroll",
                value = localSettings!!.batch_size.toString(),
                onValueChange = { localSettings = localSettings!!.copy(batch_size = it.toIntOrNull() ?: 0) },
                isLocked = lockedKeys.contains("batch_size")
            )
            SettingTextField(
                label = "Random page preload count",
                value = localSettings!!.preload_count.toString(),
                onValueChange = { localSettings = localSettings!!.copy(preload_count = it.toIntOrNull() ?: 0) },
                isLocked = lockedKeys.contains("preload_count")
            )
            SettingTextField(
                label = "Viewer click-zoom level",
                value = localSettings!!.zoom_level.toString(),
                onValueChange = { localSettings = localSettings!!.copy(zoom_level = it.toDoubleOrNull() ?: 1.0) },
                isLocked = lockedKeys.contains("zoom_level")
            )

            Text("System Settings", style = MaterialTheme.typography.titleMedium)
            SettingTextField(
                label = "Scan interval (seconds)",
                value = localSettings!!.scan_interval.toString(),
                onValueChange = { localSettings = localSettings!!.copy(scan_interval = it.toIntOrNull() ?: 0) },
                isLocked = lockedKeys.contains("scan_interval")
            )
            Text("Set to 0 to disable periodic scanning.", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))

            Button(onClick = { viewModel.saveSettings(localSettings!!, localActiveApi) }) {
                Text("Save All Settings")
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Button(onClick = { viewModel.runCacheCleanup() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Clear All Caches & Re-Scan")
            }
        }
    }
}

@Composable
private fun SettingTextField(label: String, value: String, isLocked: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = !isLocked,
        supportingText = {
            if (isLocked) {
                Text("This setting is locked by your server configuration.")
            }
        }
    )
}