package com.subhranil.clouddnsmanager.selectzones

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.selectzones.components.ZonesScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SelectZoneScreen(
    modifier: Modifier = Modifier,
    viewModel: SelectZoneViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle();
    ZonesScreen(
        zones = state.zones,
        onZoneClick = {  },
        modifier = Modifier.fillMaxSize()
    )
}