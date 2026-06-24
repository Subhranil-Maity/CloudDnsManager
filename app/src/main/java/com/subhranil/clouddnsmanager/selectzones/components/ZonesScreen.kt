package com.subhranil.clouddnsmanager.selectzones.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subhranil.clouddnsmanager.models.zone.Zone
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZonesScreen(
    zones: List<Zone>,
    isLoading: Boolean, // Added state logic parameter
    onZoneClick: (Zone) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val smoothRadius = RoundedCornerShape(8.dp)
    val primaryColor = MaterialTheme.colorScheme.primary

    val filteredZones = remember(searchQuery, zones) {
        zones.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Select Zone", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
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
            // --- Cloudflare Styled Search Field ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search your domains...") },
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

            // Dynamic layout switches using Crossfade animations
            Crossfade(targetState = isLoading, label = "ZonesScreenState") { loading ->
                if (loading) {
                    // Modern List Shimmer placeholder rows
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(5) { ZoneRowPlaceholder() }
                    }
                } else if (filteredZones.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No domains found under this account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(items = filteredZones, key = { it.id }) { zone ->
                            ZoneRow(
                                zone = zone,
                                onClick = { onZoneClick(zone) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Skeleton Placeholder Row for Zone Loading States ---
@Composable
fun ZoneRowPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Domain Name Block Track
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(width = 180.dp, height = 18.dp)
            ) {}
            Spacer(modifier = Modifier.height(8.dp))
            // Account Label Track
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(width = 110.dp, height = 12.dp)
            ) {}
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Status Alignment Track Block
        Surface(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.size(width = 65.dp, height = 14.dp)
        ) {}
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
}