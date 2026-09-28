package com.subhranil.clouddnsmanager.dns.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import com.subhranil.clouddnsmanager.dns.DnsRecordItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsRecordsScreen(
    dnsRecords: List<DnsRecordItem>,
    isLoading: Boolean, // Control system state
    modifier: Modifier = Modifier,
    refreshing: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSelectRecord: (DnsRecordItem) -> Unit,
    onAddRecord: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    // Saveable so the query survives going to the editor and back
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val smoothRadius = RoundedCornerShape(8.dp)
    val primaryColor = MaterialTheme.colorScheme.primary

    val filteredRecords = remember(searchQuery, dnsRecords) {
        dnsRecords.filter {
            it.record.name.contains(searchQuery, ignoreCase = true) ||
                    it.record.type.name.contains(searchQuery, ignoreCase = true) ||
                    it.record.content.contains(searchQuery, ignoreCase = true) ||
                    it.comment?.contains(searchQuery, ignoreCase = true) == true ||
                    it.note?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("DNS Records", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isLoading && !refreshing) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh records")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                ExtendedFloatingActionButton(
                    onClick = onAddRecord,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add record") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // --- Search Field ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search records...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                shape = smoothRadius,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Background reload after a change: keep the list, show a thin bar
            if (refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Smooth cross-fade animation when moving out of loading states

            Crossfade(targetState = isLoading, label = "ScreenState") { loading ->
                if (loading) {
                    // Modern Loading Shimmer placeholder rows
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(5) { DnsRowPlaceholder() }
                    }
                } else if (filteredRecords.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No records yet. Tap \"Add record\" to create one."
                            else "No records found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Room at the bottom so the FAB never covers the last row
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(items = filteredRecords, key = { it.record.id }) { item ->
                            DnsRecordRow(item = item, Modifier.clickable {
                                onSelectRecord(item)
                            })
                        }
                    }
                }
            }
        }
    }
}

// --- Skeleton Placeholder Row for Loading States ---
@Composable
fun DnsRowPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mock Badge Block
        Surface(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.size(width = 64.dp, height = 28.dp)
        ) {}

        Spacer(modifier = Modifier.width(16.dp))

        // Mock Metadata Block
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(width = 140.dp, height = 16.dp)
            ) {}
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(width = 200.dp, height = 12.dp)
            ) {}
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
}
