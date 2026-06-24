package com.subhranil.clouddnsmanager.zone.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subhranil.clouddnsmanager.models.dns.DnsRecord

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsRecordsScreen(
    dnsRecords: List<DnsRecord>,
    isLoading: Boolean, // Control system state
    modifier: Modifier = Modifier,
    drawer: DnsRecord?,
    onDrawerDismiss: () -> Unit,
    onSelectDrawer: (DnsRecord) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val smoothRadius = RoundedCornerShape(8.dp)
    val primaryColor = MaterialTheme.colorScheme.primary

    val filteredRecords = remember(searchQuery, dnsRecords) {
        dnsRecords.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.type.name.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("DNS Records", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
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

            Spacer(modifier = Modifier.height(8.dp))

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
                            text = "No records found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(items = filteredRecords, key = { it.id }) { record ->
                            DnsRecordRow(record = record, Modifier.clickable{
                                onSelectDrawer(record)
                            })
                        }
                    }
                }
            }
            if (drawer != null) {
                DnsRecordDetailDrawer(
                    record = drawer,
                    onDismiss = onDrawerDismiss
                )
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