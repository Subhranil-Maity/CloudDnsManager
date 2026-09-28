package com.subhranil.clouddnsmanager.selectzones

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.subhranil.clouddnsmanager.preview.PreviewTheme
import com.subhranil.clouddnsmanager.preview.SampleData
import com.subhranil.clouddnsmanager.preview.ScreenPreview
import com.subhranil.clouddnsmanager.selectzones.components.ZonesScreen

@PreviewTest
@ScreenPreview
@Composable
fun ZonesScreenPreview() {
    PreviewTheme {
        ZonesScreen(
            zones = SampleData.zones,
            isLoading = false,
            onZoneClick = {},
            onLogout = {},
            onOpenSecurity = {},
        )
    }
}
